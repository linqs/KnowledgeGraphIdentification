package edu.umd.cs.psl.LearnTheWeb;
/*
 * This file is part of the PSL software.
 * Copyright 2011 University of Maryland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *//*
 * This file is part of the PSL software.
 * Copyright 2011 University of Maryland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.psl.groovy.*;
import edu.umd.cs.psl.config.*;
import edu.umd.cs.psl.core.*;
import edu.umd.cs.psl.core.inference.*;
import edu.umd.cs.psl.ui.loading.*
import edu.umd.cs.psl.util.database.*
import edu.umd.cs.psl.evaluation.result.*;


import edu.umd.cs.psl.database.DataStore;
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DatabasePopulator;
import edu.umd.cs.psl.database.DatabaseQuery;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore;
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver;
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type;

import edu.emory.mathcs.utils.ConcurrencyUtils;
import edu.umd.cs.psl.application.inference.MPEInference;
import edu.umd.cs.psl.application.inference.LazyMPEInference;

import edu.umd.cs.psl.application.learning.weight.*;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.*;
import edu.umd.cs.psl.application.learning.weight.maxmargin.*;


import edu.umd.cs.psl.model.argument.ArgumentType
import edu.umd.cs.psl.model.predicate.Predicate
import edu.umd.cs.psl.model.predicate.StandardPredicate
import edu.umd.cs.psl.model.argument.GroundTerm
import edu.umd.cs.psl.model.argument.Variable
import edu.umd.cs.psl.model.atom.*
import edu.umd.cs.psl.model.function.AttributeSimilarityFunction

import edu.umd.cs.psl.evaluation.debug.AtomPrinter;
import edu.umd.cs.psl.evaluation.resultui.printer.AtomPrintStream;
import edu.umd.cs.psl.evaluation.resultui.printer.DefaultAtomPrintStream;

import java.io.*;
import java.util.*;
import java.util.HashSet;

Logger log = LoggerFactory.getLogger(this.class);


def printInferenceResults(datastore, readPartition){
    Partition dummy = new Partition(99);
    Database resultsDB = datastore.getDatabase(dummy, readPartition);

    AtomPrintStream printer = new DefaultAtomPrintStream();
    Set atomSet = Queries.getAllAtoms(resultsDB,Rel);
    for (Atom a : atomSet) {
	printer.printAtom(a);
    }
    atomSet = Queries.getAllAtoms(resultsDB,Cat);
    for (Atom a : atomSet) {
	printer.printAtom(a);
    }
    resultsDB.close()
}

def initializeWeightMap(){
    seedWt = 7500;
    trainWt = 5000;
    constrWt = 100;//2500;
    erWt = 10;//1200;
    candWt = 1;//200;
    pattWt = 1;//500;
    promWt = 1;//800;
    priorWtPos = 1;//80;
    priorWtNeg = 1;//80;

    weightMap = ["Sub":constrWt,
		 "RSub":constrWt,
		 "Mut":constrWt,
		 "RMut":constrWt,
		 "Inv":constrWt,
		 "Domain":constrWt,
		 "Range":constrWt,
		 "ERCat":erWt,
		 "ERRelObj":erWt,
		 "ERRelSubj":erWt,
		 "CandCat_General":candWt,
		 "CandRel_General":candWt,
		 "CandCat_CBL":candWt,
		 "CandRel_CBL":candWt,
		 "CandCat_CMC":candWt,
		 "CandRel_CMC":candWt,
		 "CandCat_CPL":candWt,
		 "CandRel_CPL":candWt,
		 "CandCat_SEAL":candWt,
		 "CandRel_SEAL":candWt,
		 "CandCat_Morph":candWt,
		 "CandRel_Morph":candWt,
		 "PromCat_General":promWt,
		 "PromRel_General":promWt,
		 "PattCat":pattWt,
		 "PattRel":pattWt,
		 "negPriorCat":priorWtNeg,
		 "negPriorRel":priorWtNeg];
    return weightMap;
}


def createModel(data,modelFeatures,weightMap){
    PSLModel m = new PSLModel(this, data);
    sqPotentials = true;
    sqOntoPotentials = true;//false;

    ///////////////////////////// rules ////////////////////////////////////
    System.out.println "[info] \t\tREADING RULES...";


    /*****************Entity Resolution******************/
    //// Label Closure
    m.add rule:  ( SameEntity(A,B) & Cat(A,C) )  >> Cat(B,C) ,
	squared: sqPotentials,
	weight : weightMap["ERCat"];

    //// Relational Closure
    m.add rule: ( SameEntity(A,B) & Rel(A,Z,R) )  >> Rel(B,Z,R) ,
	squared: sqPotentials,
	weight : weightMap["ERRelSubj"];

    m.add rule: ( SameEntity(A,B) & Rel(Z,A,R) ) >> Rel(Z,B,R) ,
	squared: sqPotentials,
	weight : weightMap["ERRelObj"];


    /*****************Ontology******************/
    //// Label Subsumption
    if(modelFeatures["useSub"] || modelFeatures["useCatSubMut"]){
	m.add rule: ( Sub(C,D) & Cat(A,C) ) >> Cat(A,D) ,        
	    //constraint : true ;
	    squared: sqOntoPotentials,
	    weight : weightMap["Sub"];
    }
    //// Relation Subsumption
    if(modelFeatures["useSub"] || modelFeatures["useRelSubMut"]){
	m.add rule: ( RSub(R,S) & Rel(A,B,R) ) >> Rel(A,B,S), 
	    //constraint : true ;
	    squared: sqOntoPotentials,
	    weight : weightMap["RSub"];
    }

    //// Label Mutual Exclusion
    if(modelFeatures["useMut"] || modelFeatures["useCatSubMut"]){
	m.add rule: ( Mut(C,D) & Cat(A,C) ) >> ~Cat(A,D),
	    //constraint : true ;
	    squared: sqOntoPotentials,
	    weight : weightMap["Mut"];
    }

    //// Relation Mutual Exclusion
    if(modelFeatures["useMut"] || modelFeatures["useRelSubMut"]){	
	m.add rule: ( RMut(R,S) & Rel(A,B,R) ) >> ~Rel(A,B,S),
	    //constraint : true ;
	    squared: sqOntoPotentials,
	    weight : weightMap["RMut"];	  
    }
    
    //// Domain/Range/Inverse
    if(modelFeatures["useDRI"]){
	m.add rule: ( Inv(R,S) & Rel(A,B,R) ) >> Rel(B,A,S),
	    //constraint : true ;
	    squared: sqOntoPotentials,
	    weight : weightMap["Inv"];	  

	m.add rule: ( Domain(R,C) & Rel(A,B,R) ) >> Cat(A,C),
	    //constraint : true ;
	    squared: sqOntoPotentials,
	    weight : weightMap["Domain"];

	m.add rule: ( Range2(R,C) & Rel(A,B,R) ) >> Cat(B,C),
	    //constraint : true ;
	    squared: sqOntoPotentials,
	    weight : weightMap["Range"];
    }

    m.add rule: ( CandCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : candWt;
    m.add rule: ( CandRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : candWt;

    m.add rule: ( CandCat_General(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_General"];
    m.add rule: ( CandRel_General(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_General"];


    m.add rule: (CandCat_CBL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CBL"];
    m.add rule: (CandRel_CBL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CBL"];

    m.add rule: (CandCat_CMC(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CMC"];
    m.add rule: (CandRel_CMC(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CMC"];

    m.add rule: (CandCat_CPL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CPL"];
    m.add rule: (CandRel_CPL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CPL"];

    m.add rule: (CandCat_Morph(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_Morph"];
    m.add rule: (CandRel_Morph(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_Morph"];

    m.add rule: (CandCat_SEAL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_SEAL"];
    m.add rule: (CandRel_SEAL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_SEAL"];

    m.add rule: (PattCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["PattCat"];
    m.add rule: (PattRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["PattCat"];


    m.add rule: (PromCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : promWt;
    m.add rule: (PromRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : promWt;

    m.add rule: (PromCat_General(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["PromCat_General"];
    m.add rule: (PromRel_General(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["PromRel_General"];

    m.add rule: ~Cat(A,C),
	squared: sqPotentials,
	weight: weightMap["negPriorCat"];

    m.add rule: ~Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["negPriorRel"];

    System.out.println(m);
    return m;
}

def runInference(m, data, factPartition){

    Partition ontology = new Partition(10);

    Partition seeds = new Partition(40);

    Partition training = new Partition(22);

    Partition writeInfAll = new Partition(35);
    data.deletePartition(writeInfAll);

    System.out.println("Starting Inference on Partition "+factPartition.getID());
    ConfigManager cm = ConfigManager.getManager();
    ConfigBundle inferenceBundle = cm.getBundle("inference");
    inferenceBundle.addProperty("admmreasoner.maxiterations",30000);
    inferenceBundle.addProperty("lazympeinference.maxrounds",14);

    HashSet closedPredsAll = new HashSet<StandardPredicate>([Name,Sub,RSub,Mut,RMut,Inv,Domain,Range2,SameEntity,CandCat,CandRel,CandCat_General,CandRel_General,CandCat_CBL,CandCat_CMC,CandCat_CPL,CandCat_Morph,CandCat_SEAL,CandRel_CBL,CandRel_CPL,CandRel_SEAL,PromCat_General,PromRel_General,SeedCat,SeedRel,TrCat,TrRel,ValCat,ValRel]);

    Database inferenceDB = data.getDatabase(writeInfAll, closedPredsAll, factPartition, ontology, training, seeds);

    mpe = new LazyMPEInference(m, inferenceDB, inferenceBundle);
    result = mpe.mpeInference();
    inferenceDB.close();

    System.out.println("STATUS: Inference complete - Partition "+factPartition.getID());
    print_results(data, writeInfAll);
}


DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, './kgi', false), new EmptyBundle());

def modelFlags =  ['useCatSubMut':1, 'useRelSubMut':0, 'useSub':0, 'useMut':0, 'useDRI':0];
def weightMap = initializeWeightMap();
PSLModel m  = createModel(data,modelFlags,weightMap); 

Partition factPartition = new Partition(30);
runInference(m, data, p);

