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


ConcurrencyUtils.setNumberOfThreads(1);



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

/*** Input Data Predicates ***/
m.add predicate: "CandCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "CandRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]



/*** Load ontology atoms ***/
Partition ontology = new Partition(10);

def ontoMap = [((Predicate)Mut):dataroot+"Mut.txt",
		((Predicate)Sub):dataroot+"Sub.txt",
		((Predicate)RSub):dataroot+"RSub.txt",
    	      	((Predicate)Domain):dataroot+"Domain.txt",
		((Predicate)Inv):dataroot+"Inv.txt",
		((Predicate)Range2):dataroot+"Range2.txt",
		((Predicate)RMut):dataroot+"RMut.txt"];

KGIUtils.loadPredicateAtoms(data, ontoMap, ontology)


/*** Load entity resolution atoms ***/
Partition entity_resolution = new Partition(30);

def erMap =     [((Predicate)SameEntity):dataroot+"1000_sameentity.noise.txt"];

KGIUtils.loadPredicateAtomsWithValue(data, erMap, entity_resolution)

/*** Load candidate atoms ***/
Partition candidates = new Partition(50);

def predMap = [((Predicate)CandCat):dataroot+"1000_cat.noise.txt",
	       ((Predicate)CandRel):dataroot+"1000_rel.noise.txt"];

KGIUtils.loadPredicateAtomsWithValue(data, predMap, candidates)

System.out.println("[info] data loading finished")

Date stop = new Date();

TimeDuration td = TimeCategory.minus( stop, start );
System.out.println td;
