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

import edu.umd.cs.psl.model.argument.ArgumentType
import edu.umd.cs.psl.model.predicate.Predicate
import edu.umd.cs.psl.model.predicate.StandardPredicate
import edu.umd.cs.psl.model.argument.GroundTerm
import edu.umd.cs.psl.model.argument.Variable
import edu.umd.cs.psl.model.atom.*

import edu.umd.cs.psl.kgi.*

import java.io.*;
import java.util.*;
import java.util.HashSet;

import groovy.time.*;

Date start = new Date();

Logger log = LoggerFactory.getLogger(this.class);

//Where the data resides (first argument to this script)
def dataroot = args[0];

DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, './psl', true), new EmptyBundle());
PSLModel m = new PSLModel(this, data);


////////////////////////// predicate declaration ////////////////////////
System.out.println "[info] \t\tDECLARING PREDICATES...";

/*** Target Predicates ***/
m.add predicate: "Cat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "Rel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]


/*** Ontology Predicates ***/				
m.add predicate: "Sub", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "RSub", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "Mut", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "RMut", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "Inv", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "Domain", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "Range2", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

/*** Entity Resolution Predicates ***/
m.add predicate: "SameEntity", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]


/*** Scoping Predicates ***/
m.add predicate: "ValCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "ValRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]


/*** Training Data Predicates ***/
m.add predicate: "PSeedCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "PSeedRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "NSeedCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "NSeedRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "TrCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "TrRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]



/*** Input Data Predicates ***/
m.add predicate: "CandCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandCat_General",  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandRel_General", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandCat_CBL",  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandRel_CBL", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandCat_CMC",  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandRel_CMC", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandCat_CPL",  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandRel_CPL", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandCat_Morph",  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandRel_Morph", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandCat_SEAL",  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandRel_SEAL", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "PromCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID];

m.add predicate: "PromRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID];

m.add predicate: "PromCat_General", types: [ArgumentType.UniqueID, ArgumentType.UniqueID];

m.add predicate: "PromRel_General", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID];

m.add predicate: "PattCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID];

m.add predicate: "PattRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID];


/*** Load ontology atoms ***/
Partition ontology = new Partition(10);

def ontoMap = [
		((Predicate)Mut):dataroot+"165.onto-wbpg.db.Mut.txt",
		((Predicate)Sub):dataroot+"165.onto-wbpg.db.Sub.txt",
		((Predicate)RSub):dataroot+"165.onto-wbpg.db.RSub.txt",
    	      	((Predicate)Domain):dataroot+"165.onto-wbpg.db.Domain.txt",
		((Predicate)Inv):dataroot+"165.onto-wbpg.db.Inv.txt",
		((Predicate)Range2):dataroot+"165.onto-wbpg.db.Range2.txt",
		((Predicate)RMut):dataroot+"165.onto-wbpg.db.RMut.txt"];

KGIUtils.loadPredicateAtoms(data, ontoMap, ontology)


/*** Load seed atoms ***/
Partition seeds = new Partition(20);

def seedMap = [((Predicate)Cat):dataroot+"seed.165.cat.uniq.out",
	       ((Predicate)Rel):dataroot+"seed.165.rel.uniq.out"];

KGIUtils.loadPredicateAtomsWithValue(data, seedMap, seeds)



/*** Load entity resolution atoms ***/
Partition entity_resolution = new Partition(30);

def erMap = [((Predicate)SameEntity):dataroot+"NELL.08m.165.cesv.csv.SameEntity.out"]

KGIUtils.loadPredicateAtomsWithValue(data, erMap, entity_resolution)


/*** Load training atoms ***/
Partition training = new Partition(40);

def trainMap = [((Predicate)Cat):dataroot+"label-train-uniq-raw-cat.db.TRAIN",
		((Predicate)Rel):dataroot+"label-train-uniq-raw-rel.db.TRAIN"];

KGIUtils.loadPredicateAtomsWithValue(data, trainMap, training)

/*** Load candidate atoms ***/
Partition candidates = new Partition(50);
Partition candidates_nosource = new Partition(55);

def predCatMap = [((Predicate)CandCat_CBL):dataroot+"NELL.08m.165.cesv.csv.CandCat_CBL.out",
    	       	 ((Predicate)CandCat_CMC):dataroot+"NELL.08m.165.cesv.csv.CandCat_CMC.out",
		 ((Predicate)CandCat_CPL):dataroot+"NELL.08m.165.cesv.csv.CandCat_CPL.out",
		 ((Predicate)CandCat_General):dataroot+"NELL.08m.165.cesv.csv.CandCat_General.out",
		 ((Predicate)CandCat_Morph):dataroot+"NELL.08m.165.cesv.csv.CandCat_Morph.out",
		 ((Predicate)CandCat_SEAL):dataroot+"NELL.08m.165.cesv.csv.CandCat_SEAL.out",
		 ((Predicate)PattCat):dataroot+"NELL.08m.165.cesv.csv.PattCat.out",
		 ((Predicate)PromCat_General):dataroot+"NELL.08m.165.esv.csv.PromCat_General.out"]

def predRelMap = [((Predicate)CandRel_CBL):dataroot+"NELL.08m.165.cesv.csv.CandRel_CBL.out",
    	       	 ((Predicate)CandRel_CPL):dataroot+"NELL.08m.165.cesv.csv.CandRel_CPL.out",
		 ((Predicate)CandRel_General):dataroot+"NELL.08m.165.cesv.csv.CandRel_General.out",
		 ((Predicate)CandRel_SEAL):dataroot+"NELL.08m.165.cesv.csv.CandRel_SEAL.out",
		 ((Predicate)PattRel):dataroot+"NELL.08m.165.cesv.csv.PattRel.out",
		 ((Predicate)PromRel_General):dataroot+"NELL.08m.165.esv.csv.PromRel_General.out"];

def predNoSourceMap = [((Predicate)CandCat):dataroot+"NELL.08m.165.cesv.csv.CandCat.out",
    		      ((Predicate)CandRel):dataroot+"NELL.08m.165.cesv.csv.CandRel.out"];

KGIUtils.loadPredicateAtomsWithValue(data, predCatMap, candidates)
KGIUtils.loadPredicateAtomsWithValue(data, predRelMap, candidates)
KGIUtils.loadPredicateAtomsWithValue(data, predNoSourceMap, candidates_nosource)


System.out.println("[info] data loading finished")

Date stop = new Date();

TimeDuration td = TimeCategory.minus( stop, start );
System.out.println td;
