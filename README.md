KnowledgeGraphIdentification
============================

You may want to read the PSL Getting Started guide to better understand PSL: https://github.com/linqs/psl/wiki/Getting-started

akbc13:
To quickly get started:

1. Dowload the dataset: bash fetchDataset.sh

2. Build your classpath using Maven: mvn dependency:build-classpath -Dmdep.outputFile=classpath.out

3. Compile the KGI model: mvn compile

4. Load a dataset (e.g. fullonto_equal_weights): java -cp ./target/classes/edu/umd/cs/psl/kgi/:./target/classes:`cat classpath.out` edu.umd.cs.psl.kgi.LoadDataPartitions partitions/fullonto_equal_weights/

5. Run KGI: java -Xmx15800m -cp ./target/classes/edu/umd/cs/psl/kgi/:./target/classes:`cat classpath.out` edu.umd.cs.psl.kgi.RunKGIPartitions > out


For convenience, the partitions directory contains the final datasets we used for each partitioning strategy in the paper, which were produced by  generating ontology partitions, normalizing NELL data, and inducing a partitioning on candidate facts.  We also include Ruby code to partition the Knowledge Graph ontology using Metis in the scripts directory to allow experimentation with various graph partitioning strategies. Running models with up to 48 partitions using the LoadData48Partitions and RunKGI48Partitions scripts.

