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

    m.add rule:  ( ValCat(B,C) &  SameEntity(A,B) & Cat(A,C) )  >> Cat(B,C) ,
	squared: sqPotentials,
	weight: weightMap["ERCat"];

    m.add rule: ( ValRel(B,Z,R) &  SameEntity(A,B) & Rel(A,Z,R) )  >> Rel(B,Z,R) ,
	squared: sqPotentials,
	weight: weightMap["ERRelSubj"];

    m.add rule: ( ValRel(Z,B,R) &  SameEntity(A,B) & Rel(Z,A,R) ) >> Rel(Z,B,R) ,
	squared: sqPotentials,
	weight: weightMap["ERRelObj"];


    m.add rule: ( ValCat(A,D) &  Sub(C,D) & Cat(A,C) ) >> Cat(A,D) ,        
	squared: sqOntoPotentials,
	weight: weightMap["Sub"];

    m.add rule: ( ValRel(A,B,S) &  RSub(R,S) & Rel(A,B,R) ) >> Rel(A,B,S), 
	squared: sqOntoPotentials,
	weight: weightMap["RSub"];

    m.add rule: ( ValCat(A,D) &  Mut(C,D) & Cat(A,C) ) >> ~Cat(A,D),
	squared: sqOntoPotentials,
	weight: weightMap["Mut"];

    m.add rule: ( ValRel(A,B,S) &  RMut(R,S) & Rel(A,B,R) ) >> ~Rel(A,B,S),
	squared: sqOntoPotentials,
	weight: weightMap["RMut"];

    m.add rule: ( ValRel(B,A,S) &  Inv(R,S) & Rel(A,B,R) ) >> Rel(B,A,S),
	squared: sqOntoPotentials,
	weight: weightMap["Inv"];

    m.add rule: ( ValCat(A,C) &  Domain(R,C) & Rel(A,B,R) ) >> Cat(A,C),
	squared: sqOntoPotentials,
	weight: weightMap["Domain"];

    m.add rule: ( ValCat(B,C) &   Range2(R,C) & Rel(A,B,R) ) >> Cat(B,C),
	squared: sqOntoPotentials,
	weight: weightMap["Range"];

    m.add rule: ( ValCat(A,C) & PSeedCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["SeedCat"];
    m.add rule: ( ValRel(A,B,R) & PSeedRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["SeedRel"];
    m.add rule: ( ValCat(A,C) & NSeedCat(A,C) ) >> ~Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["SeedCat"];
    m.add rule: ( ValRel(A,B,R) & NSeedRel(A,B,R) ) >> ~Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["SeedRel"];



    m.add rule: ( ValCat(A,C) & CandCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat"];
    m.add rule: ( ValRel(A,B,R) & CandRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel"];

    m.add rule: ( ValCat(A,C) & CandCat_General(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_General"];
    m.add rule: ( ValRel(A,B,R) & CandRel_General(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_General"];
    
    m.add rule: (ValCat(A,C) & CandCat_CBL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CBL"];
    m.add rule: (ValRel(A,B,R) & CandRel_CBL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CBL"];

    m.add rule: (ValCat(A,C) & CandCat_CMC(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CMC"];
    m.add rule: (ValRel(A,B,R) & CandRel_CMC(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CMC"];

    m.add rule: (ValCat(A,C) & CandCat_CPL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_CPL"];
    m.add rule: (ValRel(A,B,R) & CandRel_CPL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_CPL"];

    m.add rule: (ValCat(A,C) & CandCat_Morph(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_Morph"];
    m.add rule: (ValRel(A,B,R) & CandRel_Morph(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_Morph"];

    m.add rule: (ValCat(A,C) & CandCat_SEAL(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat_SEAL"];
    m.add rule: (ValRel(A,B,R) & CandRel_SEAL(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel_SEAL"];

    m.add rule: (ValCat(A,C) & PattCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["PattCat"];
    m.add rule: (ValRel(A,B,R) & PattRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["PattRel"];

    
    m.add rule: (ValCat(A,C) & PromCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["PromCat"];
    m.add rule: (ValRel(A,B,R) & PromRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["PromRel"];

    m.add rule: (ValCat(A,C) & PromCat_General(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["PromCat_General"];
    m.add rule: (ValRel(A,B,R) & PromRel_General(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["PromRel_General"];
    

    m.add rule: (ValCat(A,C) ) >> ~Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["negPriorCat"];
    m.add rule: (ValRel(A,B,R)  ) >> ~Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["negPriorRel"];
    

    m.add rule: (ValCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["posPriorCat"];
    m.add rule: (ValRel(A,B,R)  ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["posPriorRel"];

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
    Partition trTargets = new Partition(60);
    Partition teTargets = new Partition(65);


    Partition writeInfTr = new Partition(150);
    Partition writeInfWL = new Partition(160);
    Partition writeInfTe = new Partition(170);
    Partition writeInfObs = new Partition(180);



    /*** Run inference conditioning on training data ***/
    System.out.println("STATUS: Starting Inference with Training Targets");
    Date trainingInference = new Date();

    ConfigManager cm = ConfigManager.getManager();
    ConfigBundle inferenceBundle = cm.getBundle("inference");
    inferenceBundle.addProperty("admmreasoner.maxiterations",30000);

    HashSet closedPredsAll = new HashSet<StandardPredicate>([Name,Sub,RSub,Mut,RMut,Inv,Domain,Range2,SameEntity,CandCat,CandRel,CandCat_General,CandRel_General,CandCat_CBL,CandCat_CMC,CandCat_CPL,CandCat_Morph,CandCat_SEAL,CandRel_CBL,CandRel_CPL,CandRel_SEAL,PromCat_General,PromRel_General,SeedCat,SeedRel,TrCat,TrRel,ValCat,ValRel]);

    Database inferenceDB = data.getDatabase(writeInfTr, closedPredsAll, ontology, seeds, entity_resolution, training, candidates, trTargets);
    mpe = new MPEInference(m, inferenceDB, inferenceBundle);
    result = mpe.mpeInference();
    inferenceDB.close();

    System.out.println("STATUS: Inference For Training Complete");

    /*** Perform weight learning to learn weights that approximate inference results ***/
    System.out.println("STATUS: Starting weight learning");
    Date weightLearning = new Date();


    HashSet wlClosedPreds = new HashSet<StandardPredicate>([Rel,Cat]);

    ConfigBundle wlBundle = cm.getBundle("wl");
    wlBundle.addProperty("admmreasoner.maxiterations",10000);
    wlBundle.addProperty("mpeinference.maxrounds",25);

    wlBundle.addProperty("votedperceptron.stepsize",1);
    wlBundle.addProperty("votedperceptron.numsteps",300);


    Database wlRVDB = data.getDatabase(writeInfWL, ontology, seeds, entity_resolution, candidates, trTargets);
    Database wlObsDB = data.getDatabase(writeInfObs, wlClosedPreds, ontology, seeds, entity_resolution, training, writeInfTr, candidates, trTargets);

    VotedPerceptron vp = new MaxLikelihoodMPE(m, wlRVDB, wlObsDB, wlBundle);
    vp.learn();
    
    wlRVDB.close();
    wlObsDB.close();
    
    System.out.println("STATUS: Weight Learning complete");
    System.out.println("Learned Model:");
    System.out.println(m);


    System.out.println("STATUS: Starting Inference with Testing Targets and Learned Model");
    Date testingInference = new Date();

    Database inferenceTesting = data.getDatabase(writeInfTe, closedPredsAll, ontology, seeds, entity_resolution, training, candidates, teTargets);

    mpe = new MPEInference(m, inferenceTesting, inferenceBundle);
    result = mpe.mpeInference();
    inferenceTesting.close();

    System.out.println("STATUS: Inference with learned weights complete");
    Date finished = new Date();


    print_results(data, writeInfTe);
    print_results(data, seeds);

    TimeDuration td = TimeCategory.minus( weightLearning, trainingInference );
    System.out.println "Inference on training set took "+td;
    td = TimeCategory.minus(testingInference, weightLearning);
    System.out.println "Weight learning took "+td;
    td = TimeCategory.minus(testingInference, trainingInference);
    System.out.println "Total training time "+td;
    td = TimeCategory.minus(finished, testingInference);
    System.out.println "Total testing time "+td;
}

def kgiutils = new KGIUtils();
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, './psl', false), new EmptyBundle());
model_settings = ["weight_map":kgiutils.initializeWeightMap()];
PSLModel m  = createModel(data, model_settings);
inference_settings = ["use_entity_resolution":1, "use_ontology":1, "use_sources":1];
runInference(m, data, inference_settings);
