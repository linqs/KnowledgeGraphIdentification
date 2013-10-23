KnowledgeGraphIdentification
============================

You may want to read the PSL Getting Started guide to better understand PSL: https://github.com/linqs/psl/wiki/Getting-started

iswc13:
To quickly get started:
1. Dowload the dataset: bash fetchDataset.sh
2. Build your classpath using Maven: mvn dependency:build-classpath -Dmdep.outputFile=classpath.out
3. Compile the KGI model: mvn compile
4. Load the dataset: java -cp ./target/classes/edu/umd/cs/psl/kgi/:./target/classes:`cat classpath.out` edu.umd.cs.psl.kgi.LoadData data/
5. Run KGI: java -Xmx15800m -cp ./target/classes/edu/umd/cs/psl/kgi/:./target/classes:`cat classpath.out` edu.umd.cs.psl.kgi.RunKGI > out
6. Output scores (MusicBrainz):  perl scripts/cal_f1_auc_j.pl out data/1000_cat.ground.txt data/1000_cat.noise.txt 
6. Output scores (NELL):  perl scripts/cal_f1_auc_j.pl out  data/label-test-uniq-raw-cat.db.TRAIN data/label-test-uniq-raw-rel.db.TRAIN 

