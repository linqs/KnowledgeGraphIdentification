require 'set'

# give the location of the ontology file
onto_file = "onto-wbpg.db.txt"

if ARGV.size < 3
	puts "WRONG PARAMETER"
	puts "[Usage] ruby akbc_partition.rb has_vertex_weight edge_weight_config partion_array"
	puts "        has_vertex_weight:\ttrue or false, specify whether use vertex weights defined in cat.counts.165, rel.counts.165"
	puts "        edge_weight_config:\tfile path to configuration of weights defined"
	puts "        partition_array:\tspace separated array to holding num of partitions one want to cut into"
	puts "------- e.g., create (2, 3 and 4) partitions using vertex weight (true) and edge weights in (rel_scheme.config) "
	puts "------- ruby akbc_partition.rb true rel_scheme.config 2 3 4"
	exit
end

# flag to use vertex weight or not
is_use_vertex_weight = false
is_use_vertex_weight = true if ARGV[0] == "true"

# edge weight config file
edge_weight_config = ARGV[1]

# num of instances defined in that particular vertex
$vertex_weight = {}

# then initialize the $vertex_weight hash
if is_use_vertex_weight
	puts "constructing vertex weight .. (SHOULD NOT see this if not using vertex weight)"
	open("cat.counts.165") do |file|
		file.each_line do |line|
			next if line.strip.size < 1
			num, pred = line.strip.split(" ")
			$vertex_weight[pred] = num.to_i
		end
	end
	open("rel.counts.165") do |file|
		file.each_line do |line|
			next if line.strip.size < 1
			num, pred = line.strip.split(" ")
			$vertex_weight[pred] = num.to_i
		end
	end
else
	puts "not use vertex weight .."
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

if $edge_weight.size != 4 and $edge_weight.size != 9
	puts "the edge weight file either length is 4 or 9, something WRONG happend. DOUBLE CHECK #{edge_weight_config}"
	exit
end
# the relations you don't want navigate through to build edges (e.g. stop to build a domain-range edge)
# it's based on emperical experience of partition results
SKIP_NODES = ["Relatedto", "Everypromotedthing", "Agent", "Specializationof", 
	"Mutualproxyfor", "Proxyfor", "Proxyof", "Isoneoccurrenceof", "Ismultipleof", "Latitudelongitudeof", 
	"Latitudelongitude", "Generalizationof", "Synonymfor"]

is_rel_only_partition = false
if $edge_weight.size == 4
	is_rel_only_partition = true
	puts "using skip nodes: \n#{SKIP_NODES.join(', ')}"
end

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


# build group-by-ed ontology for each category/relation
# e.g. Actor -> [Sub] : {Person, ...}
#            -> [Mut] : { ... }
def add(line, hash, key, bi_dir)
	pair = line.split(key + "(")[1].split(")")[0].split(",")
	return if SKIP_NODES.include? pair[0] or SKIP_NODES.include? pair[1]
	hash[pair[0]] ||= {}
	hash[pair[0]][key] ||= Set.new
	hash[pair[0]][key].add pair[1]
	hash[pair[1]] ||= {}
	if bi_dir
		hash[pair[1]][key] ||= Set.new
		hash[pair[1]][key].add pair[0]
	end
end

puts "processing ontology file to construct the ontology graph"
# construct the graph
open(onto_file) do |file|
	file.each_line do |line|
		if is_rel_only_partition
			if line.start_with? "!RMut"
				add(line, RELS, "!RMut", true)
			elsif line.start_with? "Sub"
				add(line, CATS, "Sub", false)

				# build the sub tree to be used later
				pair = line.split("Sub(")[1].split(")")[0].split(",")
				parent = pair[1]; child = pair[0]
				cats_tree[parent.downcase] ||= Set.new
				cats_tree[parent.downcase].add child.downcase

			elsif line.start_with? "!Mut"
				add(line, CATS, "!Mut", true)
			elsif line.start_with? "RSub"
				add(line, RELS, "RSub", false)
			elsif line.start_with? "Mut"
				add(line, CATS, "Mut", true)
			elsif line.start_with? "RMut"
				add(line, RELS, "RMut", true)
			elsif line.start_with? "Inv"
				add(line, RELS, "Inv", true)
			elsif line.start_with? "Domain"
				pair = line.split("Domain(")[1].split(")")[0].split(",")

				# build the full domain range tree
				full_relation[pair[0]] ||= {}
				full_relation[pair[0]]["domain"] = pair[1]

				next if SKIP_NODES.include? pair[0] or SKIP_NODES.include? pair[1]
				RELS[pair[0]] ||= {}
				RELS[pair[0]]["Domain"] ||= Set.new
				RELS[pair[0]]["Domain"].add pair[1]
				CATS[pair[1]] ||= {}
				CATS[pair[1]]["DomainOrRangeOf"] ||= Set.new
				CATS[pair[1]]["DomainOrRangeOf"].add pair[0]
			elsif line.start_with? "Range"
				pair = line.split("Range(")[1].split(")")[0].split(",")


				# build the full domain range tree
				full_relation[pair[0]] ||= {}
				full_relation[pair[0]]["range"] = pair[1]

				next if SKIP_NODES.include? pair[0] or SKIP_NODES.include? pair[1]
				RELS[pair[0]] ||= {}
				RELS[pair[0]]["Range"] ||= Set.new
				RELS[pair[0]]["Range"].add pair[1]
				CATS[pair[1]] ||= {}
				CATS[pair[1]]["DomainOrRangeOf"] ||= Set.new
				CATS[pair[1]]["DomainOrRangeOf"].add pair[0]		
			end
		else
			puts "[TODO] don't support rel_cat ontology graph partition in this script, use akbc_partition_fullon.rb instead"
			exit
		end
	end
end

# rel - rel - weight
rels_sim_graph = {}
rels_ids_map = {}; id = 1; 
rels_name_map = {}
RELS.keys.sort.each {|k| rels_ids_map[k] = id; rels_name_map[id] = k; id = id+1; }


mode = "vol" # cut

rmut_pairs = Set.new

def add_to_sim_graph(curr_id, graph, rel_set, weight, ids_map)
	rel_set.each do |rel_name|
		other_id = ids_map[rel_name]
		next if other_id == curr_id
		old_weight = graph[curr_id][other_id]
		graph[curr_id][other_id] = weight #unless (graph[curr_id][other_id] != nil and graph[curr_id][other_id] < weight)
		graph[curr_id][other_id] = old_weight if old_weight != nil and old_weight < weight
	end
end

# construct rel-rel graph
RELS.each_pair do |rel, vertex|
	curr_id = rels_ids_map[rel]
	rels_sim_graph[curr_id] ||= {}
	
	add_to_sim_graph(curr_id, rels_sim_graph, RELS[rel]["!RMut"], $edge_weight["!RMut"], rels_ids_map) if RELS[rel]["!RMut"] and $edge_weight["!RMut"] > 0
	add_to_sim_graph(curr_id, rels_sim_graph, RELS[rel]["RMut"], $edge_weight["RMut"], rels_ids_map) if RELS[rel]["RMut"] and $edge_weight["RMut"] > 0
	# use in the last stage to drop links
	RELS[rel]["RMut"].each {|rel_name| rmut_pairs.add [curr_id, rels_ids_map[rel_name]] } if RELS[rel]["RMut"]
	add_to_sim_graph(curr_id, rels_sim_graph, RELS[rel]["Inv"], $edge_weight["Inv"], rels_ids_map) if RELS[rel]["Inv"] and $edge_weight["Inv"] > 0
	add_to_sim_graph(curr_id, rels_sim_graph, CATS[RELS[rel]["Domain"].to_a[0]]["DomainOrRangeOf"], $edge_weight["DomainOrRangeOf"], rels_ids_map) if RELS[rel]["Domain"] and $edge_weight["DomainOrRangeOf"] > 0	
	add_to_sim_graph(curr_id, rels_sim_graph, CATS[RELS[rel]["Range"].to_a[0]]["DomainOrRangeOf"], $edge_weight["DomainOrRangeOf"], rels_ids_map) if RELS[rel]["Range"] and $edge_weight["DomainOrRangeOf"] > 0
end

# process RMut, cannot delete, due to bindly 
# rmut_pairs.each {|pair| rels_sim_graph[pair[0]][pair[1]] = nil; rels_sim_graph[pair[1]][pair[0]] = nil}
num_edges = 0; 1.upto(id-1) {|key| rels_sim_graph[key].each_pair {|other_key, weight| num_edges = num_edges + 1 if weight != nil} }

puts "performing partitioning ..."
# output the RELS Graph
output = open("idrelgraph.txt", "w")
metis_code = "001"
metis_code = "011" if is_use_vertex_weight
output.puts "#{id-1} #{num_edges/2} #{metis_code}"
open("relgraph.txt", "w") do |file|
	1.upto(id-1) do |key|
		file.print "#{rels_name_map[key]} "
		#puts "#{rels_name_map[key].downcase}: #{$vertex_weight[rels_name_map[key].downcase]}"
		if is_use_vertex_weight
			curr_vertex_weight = 0
			curr_vertex_weight = $vertex_weight[rels_name_map[key].downcase] if $vertex_weight[rels_name_map[key].downcase] != nil 
			output.print "#{curr_vertex_weight} " 
		end
		rels_sim_graph[key].each_pair do |other_key, weight|
			file.print " #{rels_name_map[other_key]} #{weight} " if weight != nil
			output.print "#{other_key} #{weight} "
		end
		file.puts
		output.puts
	end
end
output.close
partitions.each {|p| `gpmetis -objtype=#{mode} idrelgraph.txt #{p}` }


CAT_STATS = Set.new
REL_STATS = Set.new
def get_sub_tree(cats_hash, cats_tree, curr_cat)
	CAT_STATS.add curr_cat
	return if ["everypromotedthing"].include? curr_cat
	if cats_tree[curr_cat] != nil
		cats_tree[curr_cat].each do |sub_cat|
			cats_hash[sub_cat] ||= 0
			cats_hash[sub_cat] = cats_hash[sub_cat] + 1
			get_sub_tree(cats_hash, cats_tree, sub_cat)
		end			
	end
end


puts "converting partition result into defined format"
# should `cat the file` and feed into this ruby program
partitions.each do |num_part|

	mid_str = "vweight_" if is_use_vertex_weight
	output_file = open("165_rel_#{mid_str}parts_#{num_part}.txt", "w")

	#data = STDIN.read
	data = `cat idrelgraph.txt.part.#{num_part}`
	partitions = {}
	vertices = data.split.map {|x| x.to_i}
	1.upto(id-1) do |line_no|
		partitions[vertices[line_no-1]] ||= Set.new
		partitions[vertices[line_no-1]].add rels_name_map[line_no]
	end

	0.upto(partitions.keys.max) do |part|
		output_file.print "part #{part}: "
		curr_label_set = Set.new
		cat_set = {}
		partitions[part].each do |x| 
			REL_STATS.add x
			output_file.print x + ", "; 
			curr_label_set.add x.downcase;

			# add from relation and domains
			# NOTE: 2nd level abstract {"abstractthing", "item", "location", "agent"}
			# if RELS[x]["Domain"] != nil and RELS[x]["Domain"].size > 0
			#	cat_set[RELS[x]["Domain"].to_a[0].downcase] ||= 0;
			#	cat_set[RELS[x]["Domain"].to_a[0].downcase] = cat_set[RELS[x]["Domain"].to_a[0].downcase] + 1
			#end

			cat_set[full_relation[x]["domain"].downcase] ||= 0
			cat_set[full_relation[x]["domain"].downcase] = cat_set[full_relation[x]["domain"].downcase] + 1
			get_sub_tree(cat_set, cats_tree, full_relation[x]["domain"].downcase)

			#if RELS[x]["Range"] != nil and RELS[x]["Range"].size > 0
			#	cat_set[RELS[x]["Range"].to_a[0].downcase] ||= 0; 
			#	cat_set[RELS[x]["Range"].to_a[0].downcase] = cat_set[RELS[x]["Range"].to_a[0].downcase] + 1 
			#end

			cat_set[full_relation[x]["range"].downcase] ||= 0
			cat_set[full_relation[x]["range"].downcase] = cat_set[full_relation[x]["range"].downcase] + 1
			get_sub_tree(cat_set, cats_tree, full_relation[x]["range"].downcase)

		end

#		puts jaccd(curr_label_set, labelset); puts; 
		output_file.print "\n-- category: "
		cat_set.sort_by {|x,c| c*-1}.each {|cat_cnt| output_file.print "#{cat_cnt[0]}-#{cat_cnt[1]}, "}
#		puts jaccd(Set.new(cat_set.keys), labelcatset); 
		output_file.puts; output_file.puts;
	end

	output_file.close
end

# remove intermediate files
`rm idrelgraph.txt*`
`rm relgraph.txt`
`mkdir -p result`
`mv 165_rel* result/`