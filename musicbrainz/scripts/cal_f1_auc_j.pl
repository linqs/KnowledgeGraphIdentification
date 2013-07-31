use Data::Dumper;

print "Usage: $0 results trCats trRels\n";

my $resultsFile = shift @ARGV;
my $trueCatsFile = shift @ARGV;
my $trueRelsFile = shift @ARGV;

my $threshs = [0, .25, .375, .5, .55, .6, .65, .7, .75, .8, .85, .9, .95,  1];

my $evals_relation_ids = {};
my $evals_category_ids = {};

my $factHash = {};
my $relHash = {};
my $catHash = {};
my $trueFactHash = {};
my $trueCatHash = {};
my $trueRelHash = {};
my $names = {};
my $namesInv = {};

sub loadTrueCats {
    my $fn = shift;
    my $catHash = shift;
    my $factHash = shift;
    open(F,$fn);
    my $ctr = 0;
    while(<F>){
	chomp;
	my ($entity,$category,$label) = split("\t",$_,3);
	my $key = "$entity,$category,0";
	my $trVal = $label;
	$ctr++;
	$factHash->{$key}=$trVal;
	$catHash->{$key}=$trVal;
    }
    print "Loaded $ctr lines from $fn\n";
    close(F);

}

sub loadTrueRels {
    my $fn = shift;
    my $relHash = shift;
    my $factHash = shift;
    open(F,$fn);
    my $ctr=0;
    while(<F>){
	chomp;
	my ($entity,$value,$relation,$label) = split("\t",$_,4);
	my $key = "$entity,$value,$relation";
	my $trVal = $label;
	$ctr++;
	$factHash->{$key}=$trVal;
	$relHash->{$key}=$trVal;
    }
    print "Loaded $ctr lines from $fn\n";
    close(F);

}

sub loadResultsFile {
   my $fn = shift;
    open(F,$fn);
    while(<F>){
	chomp;
	if(/^Rel\((\d+),\s*(\d+),\s*(\d+)\)\s+\w+=\[(\S+)\]/i){
	    my $key = "$1,$2,$3";
	    $relHash->{$key} = $4;
	    $factHash->{$key} = $4;
	} elsif(/^Cat\((\d+),\s*(\d+)\)\s+\w+=\[(\S+)\]/i){
	    my $key = "$1,$2,0";
	    $catHash->{$key} = $3;
	    $factHash->{$key} = $3;
	}
    }
}

sub calcF1 {
    my $trueHash = shift;
    my $inferHash = shift;
    my $thresh = shift;

    my $truePos = 0;
    my $falsePos = 0;
    my $total = 0;
    my $f1 = 0.0;

    my $allTrue = 0;
    foreach my $k (keys %$trueHash) {$allTrue+=$trueHash->{$k}; } 

    my $prevPrec = 1;
    my $prevRecall = 0;

    foreach my $fact (keys %$inferHash){
	next unless $inferHash->{$fact} >= $thresh;
	if($trueHash->{$fact}>0)  {
	    $truePos++;
	} elsif (defined($trueHash->{$fact})){
	    $falsePos++;
	}
    }
    my $total = $truePos+$falsePos;
    my $precision, $recall;
    if($total > 0){
	$recall = $truePos/$allTrue;
	$precision = $truePos/$total;
	$f1 = 2.0 * $precision * $recall /(0.0 + $precision + $recall);
    } else {
	print STDERR "No facts found for threshold $thresh!\n";
    }
    

    return {F1 => $f1, PRECISION => $precision, RECALL => $recall};

}

sub calcAUC {
    my $trueHash = shift;
    my $inferHash = shift;

    my $truePos = 0;
    my $falsePos = 0;
    my $total = 0;
    my $auc = 0.0;
    my $prEq = 0;

    my $allTrue = 0;
    foreach my $k (keys %$trueHash) {$allTrue+=$trueHash->{$k}; } 

    my $prevPrec = 1;
    my $prevRecall = 0;

    foreach my $fact (sort {$inferHash->{$b} <=> $inferHash->{$a}} keys %$inferHash ){
	if($trueHash->{$fact}>0)  {
	    $truePos++;
	} elsif (defined($trueHash->{$fact})){
	    $falsePos++;
	}
	my $total = $truePos + $falsePos;
	if($total > 0){
	    my $recall = $truePos/$allTrue;
	    my $precision = $truePos/$total;

	    $auc += ($recall - $prevRecall) * (($precision + $prevPrec)/2.0);
	    $prevPrec = $precision;
	    $prevRecall = $recall;
	}
    }
    $auc += (1 - $prevRecall) * (($precision + 0 )/2.0);
    return $auc;
}

print "Parsing true categories... ";
loadTrueCats($trueCatsFile, $trueCatHash, $trueFactHash);
print " found ",scalar(keys %$trueCatHash),"\n";

print "Parsing true relations.... ";
loadTrueRels($trueRelsFile, $trueRelHash, $trueFactHash);
print " found ",scalar(keys %$trueRelHash),"\n";


print "Loading results file... ";
loadResultsFile($resultsFile);
print " found ",scalar(keys %$factHash),"\n";


print "Calculating AUC\n";
my $auc = calcAUC($trueFactHash,$factHash);
print "AUC: $auc\n";
print sprintf("%10s %10s %10s %10s\n", "threshold", "F1", "Precision", "Recall");
foreach my $thresh (@$threshs){
    my $res = calcF1($trueFactHash,$factHash,$thresh);
    print sprintf("%10.3f %10.3f %10.3f %10.3f\n", $thresh, $res->{F1}, $res->{PRECISION}, $res->{RECALL});
}
my $relAuc = calcAUC($trueRelHash,$relHash);
print "Relation AUC: $relAuc\n";
print sprintf("%10s %10s %10s %10s\n", "threshold", "Rel F1", "Rel Precision", "Rel Recall");
foreach my $thresh (@$threshs){
    my $res = calcF1($trueRelHash,$relHash,$thresh);
    print sprintf("%10.3f %10.3f %10.3f %10.3f\n", $thresh, $res->{F1}, $res->{PRECISION}, $res->{RECALL});
}
my $catAuc = calcAUC($trueCatHash,$catHash);
print "Category AUC: $catAuc\n";
print sprintf("%10s %10s %10s %10s\n", "threshold", "Cat F1", "Cat Precision", "Cat Recall");
foreach my $thresh (@$threshs){
    my $res = calcF1($trueCatHash,$catHash,$thresh);
    print sprintf("%10.3f %10.3f %10.3f %10.3f\n", $thresh, $res->{F1}, $res->{PRECISION}, $res->{RECALL});
}



