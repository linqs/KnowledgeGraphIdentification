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


    m.add rule: ( CandCat(A,C) ) >> Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["CandCat"];
    m.add rule: ( CandRel(A,B,R) ) >> Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["CandRel"];

    /*
    m.add rule:  ~Cat(A,C),
	squared: sqPotentials,
	weight : weightMap["negPriorCat"];
    m.add rule:  ~Rel(A,B,R),
	squared: sqPotentials,
	weight : weightMap["negPriorRel"];
    */
    System.out.println(m);
    return m;
}


def runInference(m, data, settings){

    Partition dummy = new Partition(99);

    Partition ontology = settings["use_ontology"] ? new Partition(10) : dummy;
    Partition entity_resolution = settings["use_entity_resolution"] ? new Partition(30) : dummy;
    Partition candidates = new Partition(50);

    Partition writeInfTe = new Partition(170);

    System.out.println("STATUS: Starting Inference with Testing Targets and Learned Model");
    Date testingInference = new Date();


    ConfigManager cm = ConfigManager.getManager();
    ConfigBundle inferenceBundle = cm.getBundle("inference");
    inferenceBundle.addProperty("admmreasoner.maxiterations",30000);
    inferenceBundle.addProperty("lazympeinference.maxrounds",14);


    HashSet closedPredsAll = new HashSet<StandardPredicate>([Name,Sub,RSub,Mut,RMut,Inv,Domain,Range2,SameEntity,CandCat,CandRel]);
    Database inferenceTesting = data.getDatabase(writeInfTe, closedPredsAll, candidates, ontology, entity_resolution);
    mpe = new LazyMPEInference(m, inferenceTesting, inferenceBundle);
    result = mpe.mpeInference();
    inferenceTesting.close();


    System.out.println("STATUS: Inference on test set complete");
    Date finished = new Date();

    print_results(data, writeInfTe);

    TimeDuration td = TimeCategory.minus( finished, testingInference );
    System.out.println "Total testing time "+td;
}

def kgiutils = new KGIUtils();
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, './psl', false), new EmptyBundle());
model_settings = ["weight_map":kgiutils.initializeWeightMap()];
PSLModel m  = createModel(data, model_settings);
inference_settings = ["use_entity_resolution":1, "use_ontology":1];
runInference(m, data, inference_settings);