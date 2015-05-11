require 'set'

# give the location of the ontology file
onto_file = "onto-wbpg.db.txt"

if ARGV.size < 3
	puts "WRONG PARAMETER"
	puts "[Usage] ruby akbc_partition_fullonto.rb has_vertex_weight edge_weight_config partion_array"
	puts "        has_vertex_weight:\ttrue or false, specify whether use vertex weights defined in cat.counts.165, rel.counts.165"
	puts "        edge_weight_config:\tfile path to configuration of weights defined"
	puts "        partition_array:\tspace separated array to holding num of partitions one want to cut into"
	puts "------- e.g., create (2, 3 and 4) partitions using vertex weight (true) and edge weights in (rel_scheme.config) "
	puts "------- ruby akbc_partition.rb true cat_rel_scheme.config 2 3 4"
	exit
end

# flag to use vertex weight or not
is_use_vertex_weight = false
is_use_vertex_weight = true if ARGV[0] == "true"

# edge weight config file
edge_weight_config = ARGV[1]

# num of instances defined in that particular vertex
$vertex_weight_cats = {}
$vertex_weight_rels = {}
$vertex_weight = {}

# then initialize the $vertex_weight hash
if is_use_vertex_weight
	puts "use vertex weight .. (SHOULD NOT see this if not using vertex weight)"
else
	puts "not use vertex weight .."
end	

open("cat.counts.165") do |file|
	file.each_line do |line|
		next if line.strip.size < 1
		num, pred = line.strip.split(" ")
		$vertex_weight_cats[pred] = num.to_i
		$vertex_weight[pred] = num.to_i
	end
end

open("rel.counts.165") do |file|
	file.each_line do |line|
		next if line.strip.size < 1
		num, pred = line.strip.split(" ")
		$vertex_weight_rels[pred] = num.to_i
		$vertex_weight[pred] = num.to_i
	end
end

# assignment of edge weight to a particular type of edge
$edge_weight = {}
open(edge_weight_config) do |file|
	puts "constructing edge weights .. DOUBLE CHECK the weights:"
	file.each_line do |line|
		next if line.strip.size < 1 or line.start_with? "#"
		onto_type, w = line.strip.split("\t")
		$edge_weight[onto_type.strip] = w.to_i
		puts "-- #{onto_type}: #{w.to_i}"
	end
end

if $edge_weight.size != 9
	puts "the edge weight file should have length of 9, something WRONG happend. DOUBLE CHECK #{edge_weight_config}"
	exit
end

# no need to use skip nodes this time, all nodes will be in one partition instead

# parse number of partitions
partitions = []
ARGV[2..-1].each {|p| partitions << p.to_i}
if partitions.size < 1
	puts "the number of partition cannot be 0, double check USAGE"
	exit
else
	print "will do partition ["	
	has_comma = true; i = 0
	has_comma = false if partitions.size == 1
	partitions.each {|p| print "#{p}#{',' if has_comma}"; i = i + 1; has_comma=(i != partitions.size - 1)}
	print "]"
	puts
end

# hold the rels and cats intermediate structure when construct the ontology graph and its related info
RELS = {}; CATS = {}

# category sub tree structure
# a_parent -> {a_child}
cats_tree = {}

# for domain, range
# a_relation -> [Domain]: its domain
#  			 -> [Range]: its range
full_relation = {}


# the ontology graph
# id1 -> [onto]: {ids}
onto_graph = {}
# id map for that
ids_map = {}; id = 1;
names_map = {};

puts "processing ontology file to construct the ontology graph"
# construct the graph
open(onto_file) do |file|
	file.each_line do |line|
		onto_type, fact = line.split("(")
		pair = fact.split(")")[0].split(",").map{|x| x.strip}
		#puts "#{onto_type}, #{pair[0]}, #{pair[1]}"
		# build the id for 
		if ids_map[pair[0]] == nil
			ids_map[pair[0]] = id
			names_map[id] = pair[0]
			id = id + 1
		end
		id0 = ids_map[pair[0]]
		if ids_map[pair[1]] == nil
			ids_map[pair[1]] = id
			names_map[id] = pair[1]
			id = id + 1;
		end
		id1 = ids_map[pair[1]]

		# build the graph, and add up the weights
		# only build that edge if the onto_type has positive weight
		if $edge_weight[onto_type] > 0
			#puts "#{onto_type}: #{$edge_weight[onto_type]}"
			onto_graph[id0] ||= {}
			onto_graph[id0][id1] ||= 0
			onto_graph[id0][id1] = onto_graph[id0][id1] + $edge_weight[onto_type]
			onto_graph[id1] ||= {}
			onto_graph[id1][id0] ||= 0
			onto_graph[id1][id0] = onto_graph[id1][id0] + $edge_weight[onto_type]
		end
	end
end

num_edges = 0
# note, ignore the :type attribute
onto_graph.each_pair {|key, ids| num_edges = ids.size + num_edges }

puts "performing partitioning ..."
# output the Graph
output = open("idcatrelgraph.txt", "w")
mode = "vol" # cut
metis_code = "001"
metis_code = "011" if is_use_vertex_weight
output.puts "#{id-1} #{num_edges/2} #{metis_code}"
1.upto(id-1) do |key|
	#puts "#{key} - #{names_map[key]}"
	if is_use_vertex_weight
		curr_vertex_weight = 0
		curr_vertex_weight = $vertex_weight[names_map[key].downcase] if $vertex_weight[names_map[key].downcase] != nil 
		output.print "#{curr_vertex_weight} " 
	end
	onto_graph[key].each_pair { |other_key, weight| output.print "#{other_key} #{weight} " } if onto_graph[key] != nil
	output.puts
end
output.close
partitions.each {|p| `gpmetis -objtype=#{mode} idcatrelgraph.txt #{p}` }

puts "converting partition result into defined format"
partitions.each do |num_part|

	mid_str = "vweight_" if is_use_vertex_weight
	output_file = open("165_fullonto_#{mid_str}parts_#{num_part}.txt", "w")

	data = `cat idcatrelgraph.txt.part.#{num_part}`
	partitions = {}
	vertices = data.split.map {|x| x.to_i}
	1.upto(id-1) do |line_no|
		partitions[vertices[line_no-1]] ||= Set.new
		partitions[vertices[line_no-1]].add names_map[line_no]
	end

	0.upto(partitions.keys.max) do |part|
		output_file.print "part #{part}: "

		partitions[part].each do |x|
			if $vertex_weight_rels.include? x.downcase
				output_file.print "#{x}, "
			end
		end
		output_file.print "\n-- category: "
		partitions[part].each do |x|
			if $vertex_weight_cats.include? x.downcase
				output_file.print "#{x.downcase}-1, "
			end
		end
		output_file.puts; output_file.puts
	end

	output_file.close
end

# remove intermediate files
`rm idcatrelgraph.txt*`
`mkdir -p result`
`mv 165_fullonto* result/`
