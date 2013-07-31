package edu.umd.cs.psl.kgi;
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

import edu.umd.cs.psl.evaluation.debug.AtomPrinter;
import edu.umd.cs.psl.evaluation.resultui.printer.AtomPrintStream;
import edu.umd.cs.psl.evaluation.resultui.printer.DefaultAtomPrintStream;

import edu.umd.cs.psl.kgi.*

import java.io.*;
import java.util.*;
import java.util.HashSet;

import groovy.time.*;

Logger log = LoggerFactory.getLogger(this.class);

def print_results(datastore, readPartition){
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
    resultsDB.close();
}


def createModel(data, settings){
    PSLModel m = new PSLModel(this, data);

    weightMap = settings["weight_map"];

    ontoConstraints = false;
    sqPotentials = true;
    sqOntoPotentials = true;//false;


    ///////////////////////////// rules ////////////////////////////////////
    System.out.println "[info] \t\tREADING RULES...";

    m.add rule:  ( SameEntity(A,B) & Cat(A,C) )  >> Cat(B,C) ,
	squared: sqPotentials,
	weight: weightMap["ERCat"];

    m.add rule: ( SameEntity(A,B) & Rel(A,Z,R) )  >> Rel(B,Z,R) ,
	squared: sqPotentials,
	weight: weightMap["ERRelSubj"];

    m.add rule: ( SameEntity(A,B) & Rel(Z,A,R) ) >> Rel(Z,B,R) ,
	squared: sqPotentials,
	weight: weightMap["ERRelObj"];


    m.add rule: ( Sub(C,D) & Cat(A,C) ) >> Cat(A,D) ,        
	squared: sqOntoPotentials,
	weight: weightMap["Sub"];

    m.add rule: ( RSub(R,S) & Rel(A,B,R) ) >> Rel(A,B,S), 
	squared: sqOntoPotentials,
	weight: weightMap["RSub"];

    m.add rule: ( Mut(C,D) & Cat(A,C) ) >> ~Cat(A,D),
	squared: sqOntoPotentials,
	weight: weightMap["Mut"];

    m.add rule: ( RMut(R,S) & Rel(A,B,R) ) >> ~Rel(A,B,S),
	squared: sqOntoPotentials,
	weight: weightMap["RMut"];

    m.add rule: ( Inv(R,S) & Rel(A,B,R) ) >> Rel(B,A,S),
	squared: sqOntoPotentials,
	weight: weightMap["Inv"];

    m.add rule: ( Domain(R,C) & Rel(A,B,R) ) >> Cat(A,C),
	squared: sqOntoPotentials,
	weight: weightMap["Domain"];

    m.add rule: ( Range2(R,C) & Rel(A,B,R) ) >> Cat(B,C),
	squared: sqOntoPotentials,
	weight: weightMap["Range"];

    m.add rule: ( PSeedCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["SeedCat"];
    m.add rule: ( PSeedRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["SeedRel"];
    m.add rule: (NSeedCat(A,C) ) >> ~Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["SeedCat"];
    m.add rule: (NSeedRel(A,B,R) ) >> ~Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["SeedRel"];



    m.add rule: ( CandCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat"];
    m.add rule: ( CandRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel"];

    m.add rule: ( CandCat_General(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_General"];
    m.add rule: ( CandRel_General(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_General"];
    
    m.add rule: ( CandCat_CBL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CBL"];
    m.add rule: ( CandRel_CBL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CBL"];

    m.add rule: ( CandCat_CMC(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CMC"];
    m.add rule: ( CandRel_CMC(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CMC"];

    m.add rule: ( CandCat_CPL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CPL"];
    m.add rule: ( CandRel_CPL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CPL"];

    m.add rule: ( CandCat_Morph(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_Morph"];
    m.add rule: ( CandRel_Morph(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_Morph"];

    m.add rule: ( CandCat_SEAL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_SEAL"];
    m.add rule: ( CandRel_SEAL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_SEAL"];

    m.add rule: ( PattCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["PattCat"];
    m.add rule: ( PattRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["PattRel"];

    
    m.add rule: ( PromCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["PromCat"];
    m.add rule: ( PromRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["PromRel"];

    m.add rule: ( PromCat_General(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["PromCat_General"];
    m.add rule: ( PromRel_General(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["PromRel_General"];
    

    m.add rule: ~Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["negPriorCat"];
    m.add rule: ~Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["negPriorRel"];
    

    System.out.println(m);
    return m;
}

def runInference(m, data, settings){

    Partition dummy = new Partition(99);

    Partition ontology = settings["use_ontology"] ? new Partition(10) : dummy;
    Partition seeds = new Partition(20);
    Partition entity_resolution = settings["use_entity_resolution"] ? new Partition(30) : dummy;
    Partition training = new Partition(40);
    Partition candidates = settings["use_sources"] ? new Partition(50) : new Partition(55);

    Partition writeInfTe = new Partition(170);

    /*** Run inference conditioning on training data ***/
    System.out.println("STATUS: Starting Inference with Training Targets");
    Date trainingInference = new Date();

    ConfigManager cm = ConfigManager.getManager();
    ConfigBundle inferenceBundle = cm.getBundle("inference");
    inferenceBundle.addProperty("admmreasoner.maxiterations",30000);
    inferenceBundle.addProperty("lazympeinference.maxrounds",14);

    HashSet closedPredsAll = new HashSet<StandardPredicate>([Name,Sub,RSub,Mut,RMut,Inv,Domain,Range2,SameEntity,CandCat,CandRel,CandCat_General,CandRel_General,CandCat_CBL,CandCat_CMC,CandCat_CPL,CandCat_Morph,CandCat_SEAL,CandRel_CBL,CandRel_CPL,CandRel_SEAL,PromCat_General,PromRel_General,SeedCat,SeedRel,TrCat,TrRel]);

    System.out.println("STATUS: Starting Inference with Testing Targets and Learned Model");
    Date testingInference = new Date();

    Database inferenceTesting = data.getDatabase(writeInfTe, closedPredsAll, ontology, seeds, entity_resolution, training, candidates);

    mpe = new LazyMPEInference(m, inferenceTesting, inferenceBundle);
    result = mpe.mpeInference();
    inferenceTesting.close();

    System.out.println("STATUS: Inference with learned weights complete");
    Date finished = new Date();


    print_results(data, writeInfTe);
    print_results(data, seeds);

    td = TimeCategory.minus(finished, testingInference);
    System.out.println "Total testing time "+td;
}

def kgiutils = new KGIUtils();
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, './psl', false), new EmptyBundle());

/*** These weights are derived from the non-lazy NELL experiment. While the KGI problem setting for lazy and non-lazy inference is completely different, we found that using the learned model after weight learning in the non-lazy settings improved our results.  ****/

def weightMap = ["Sub":100,
		 "RSub":99.95404470039401,
		 "Mut":23.1761207378166,
		 "RMut":100,
		 "Inv":99.94383801470507,
		 "Domain":99.93756554876204,
		 "Range":99.91164648719196,
		 "ERCat":24.317360824212926,
		 "ERRelObj":24.268752804084627,
		 "ERRelSubj":24.167434982886157,
		 "SeedCat":1000,
		 "SeedRel":1000,
		 "CandCat":1,
		 "CandRel":1,
		 "CandCat_General":1.9247474887642053,
		 "CandRel_General":0.05093475899009211,
		 "CandCat_CBL":0.0696106103638450,
		 "CandRel_CBL":0.026966648574574432,
		 "CandCat_CMC":0.006874172024394287,
		 "CandRel_CMC":1,
		 "CandCat_CPL":0.012402610094926799,
		 "CandRel_CPL":0.024562654639784076,
		 "CandCat_SEAL":0.018952544686577175,
		 "CandRel_SEAL":0.08174686570602384,
		 "CandCat_Morph":0.034145000518647495,
		 "CandRel_Morph":1,
		 "PromCat":1,
		 "PromRel":1,
		 "PromCat_General":0.03488693263677389,
		 "PromRel_General":0.0454172382866946,
		 "PattCat":0.12767258704250645,
		 "PattRel":0.1558933729391663,
		 "posPriorCat":1,
		 "posPriorRel":1,
		 "negPriorCat":0.024361295837807036,
		 "negPriorRel":0];

model_settings = ["weight_map":weightMap];
PSLModel m  = createModel(data, model_settings);
inference_settings = ["use_entity_resolution":1, "use_ontology":1, "use_sources":1];
runInference(m, data, inference_settings);
