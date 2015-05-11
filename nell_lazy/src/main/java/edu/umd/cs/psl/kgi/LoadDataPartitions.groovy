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

import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.LazyMaxLikelihoodMPE;
import edu.umd.cs.psl.application.learning.weight.maxmargin.MaxMargin;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxPseudoLikelihood;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.VotedPerceptron;

import edu.umd.cs.psl.model.argument.ArgumentType
import edu.umd.cs.psl.model.predicate.Predicate
import edu.umd.cs.psl.model.predicate.StandardPredicate
import edu.umd.cs.psl.model.argument.GroundTerm
import edu.umd.cs.psl.model.argument.Variable
import edu.umd.cs.psl.model.atom.*

import java.io.*;
import java.util.*;
import java.util.HashSet;

import groovy.time.*;
Date start = new Date();

Logger log = LoggerFactory.getLogger(this.class);


ConcurrencyUtils.setNumberOfThreads(1);

DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, './psl', true), new EmptyBundle());
PSLModel m = new PSLModel(this, data);

////////////////////////// predicate declaration ////////////////////////
log.info("DECLARING PREDICATES...");

m.add predicate: "Name", types: [ArgumentType.UniqueID,ArgumentType.String]
				
// going to infer the value for cat, and compare with hand-labelled
m.add predicate: "Cat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
// going to infer the value for rel, and compare with hand-labelled
m.add predicate: "Rel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]
				
m.add predicate: "Sub", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "RSub", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "Mut", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "RMut", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "Inv", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "Domain", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "SameEntity", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

// call it range2, due to psl will report error when using range!!, a keyword?
m.add predicate: "Range2", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "ValCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "ValRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "SeedCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "SeedRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "TrCat", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "TrRel", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]

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

Partition ontology = new Partition(10);

def dataroot = args[0];

def ontoMap = [
		((Predicate)Mut):dataroot+"onto-wbpg.db.Mut.txt",
		((Predicate)Sub):dataroot+"onto-wbpg.db.Sub.txt",
		((Predicate)RSub):dataroot+"onto-wbpg.db.RSub.txt",
    	      	((Predicate)Domain):dataroot+"onto-wbpg.db.Domain.txt",
		((Predicate)Inv):dataroot+"onto-wbpg.db.Inv.txt",
		((Predicate)Range2):dataroot+"onto-wbpg.db.Range2.txt",
		((Predicate)RMut):dataroot+"onto-wbpg.db.RMut.txt"];

def pMap = ontoMap;

for (Predicate p : pMap.keySet() ){
    insert = data.getInserter(p,ontology);
    log.info("Loading files "+pMap[p]);
    InserterUtils.loadDelimitedData(insert,pMap[p]);
}


// load the seed data
Partition seeds = new Partition(40);

insert = data.getInserter(Cat, seeds);
log.info("Loading files seed.165.cat.uniq.out");
InserterUtils.loadDelimitedDataTruth(insert,dataroot+"seed-conv-raw-cat.db");
insert = data.getInserter(Rel, seeds);
log.info("Loading files seed.165.rel.uniq.out");
InserterUtils.loadDelimitedDataTruth(insert,dataroot+"seed-conv-raw-rel.db");

Partition facts0 = new Partition(30);
Partition facts1 = new Partition(31);
Partition facts2 = new Partition(32);
Partition facts3 = new Partition(33);
Partition facts4 = new Partition(34);
Partition facts5 = new Partition(35);
Partition facts012345 = new Partition(36);
Partition facts012 = new Partition(137);
Partition facts345 = new Partition(139);
Partition facts01 = new Partition(37);
Partition facts23 = new Partition(38);
Partition facts45 = new Partition(39);


def pMap012345 = [((Predicate)CandCat_CBL):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Cat_CBL.out",
    	    ((Predicate)CandCat_CMC):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Cat_CMC.out",
	    ((Predicate)CandCat_CPL):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Cat_CPL.out",
	    ((Predicate)CandCat_General):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Cat_General.out",
	    ((Predicate)CandCat_Morph):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Cat_Morph.out",
	    ((Predicate)CandCat_SEAL):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Cat_SEAL.out",
	    ((Predicate)CandRel_CBL):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Rel_CBL.out",
	    ((Predicate)CandRel_CPL):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Rel_CPL.out",
	    ((Predicate)CandRel_General):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Rel_General.out",
	    ((Predicate)CandRel_SEAL):dataroot+"NELL.part012345.165.cesv.csv.Cand012345Rel_SEAL.out",
	    ((Predicate)PromCat_General):dataroot+"NELL.part012345.165.esv.csv.Prom012345Cat_General.out",
	    ((Predicate)PromRel_General):dataroot+"NELL.part012345.165.esv.csv.Prom012345Rel_General.out"];

def pMap0 = [((Predicate)CandCat_CBL):dataroot+"NELL.part0.165.cesv.csv.Cand0Cat_CBL.out",
    	    ((Predicate)CandCat_CMC):dataroot+"NELL.part0.165.cesv.csv.Cand0Cat_CMC.out",
	    ((Predicate)CandCat_CPL):dataroot+"NELL.part0.165.cesv.csv.Cand0Cat_CPL.out",
	    ((Predicate)CandCat_General):dataroot+"NELL.part0.165.cesv.csv.Cand0Cat_General.out",
	    ((Predicate)CandCat_Morph):dataroot+"NELL.part0.165.cesv.csv.Cand0Cat_Morph.out",
	    ((Predicate)CandCat_SEAL):dataroot+"NELL.part0.165.cesv.csv.Cand0Cat_SEAL.out",
	    ((Predicate)CandRel_CBL):dataroot+"NELL.part0.165.cesv.csv.Cand0Rel_CBL.out",
	    ((Predicate)CandRel_CPL):dataroot+"NELL.part0.165.cesv.csv.Cand0Rel_CPL.out",
	    ((Predicate)CandRel_General):dataroot+"NELL.part0.165.cesv.csv.Cand0Rel_General.out",
	    ((Predicate)CandRel_SEAL):dataroot+"NELL.part0.165.cesv.csv.Cand0Rel_SEAL.out",
	    ((Predicate)PromCat_General):dataroot+"NELL.part0.165.esv.csv.Prom0Cat_General.out",
	    ((Predicate)PromRel_General):dataroot+"NELL.part0.165.esv.csv.Prom0Rel_General.out"];

def pMap1 = [((Predicate)CandCat_CBL):dataroot+"NELL.part1.165.cesv.csv.Cand1Cat_CBL.out",
    	    ((Predicate)CandCat_CMC):dataroot+"NELL.part1.165.cesv.csv.Cand1Cat_CMC.out",
	    ((Predicate)CandCat_CPL):dataroot+"NELL.part1.165.cesv.csv.Cand1Cat_CPL.out",
	    ((Predicate)CandCat_General):dataroot+"NELL.part1.165.cesv.csv.Cand1Cat_General.out",
	    ((Predicate)CandCat_Morph):dataroot+"NELL.part1.165.cesv.csv.Cand1Cat_Morph.out",
	    ((Predicate)CandCat_SEAL):dataroot+"NELL.part1.165.cesv.csv.Cand1Cat_SEAL.out",
	    ((Predicate)CandRel_CBL):dataroot+"NELL.part1.165.cesv.csv.Cand1Rel_CBL.out",
	    ((Predicate)CandRel_CPL):dataroot+"NELL.part1.165.cesv.csv.Cand1Rel_CPL.out",
	    ((Predicate)CandRel_General):dataroot+"NELL.part1.165.cesv.csv.Cand1Rel_General.out",
	    ((Predicate)CandRel_SEAL):dataroot+"NELL.part1.165.cesv.csv.Cand1Rel_SEAL.out",
	    ((Predicate)PromCat_General):dataroot+"NELL.part1.165.esv.csv.Prom1Cat_General.out",
	    ((Predicate)PromRel_General):dataroot+"NELL.part1.165.esv.csv.Prom1Rel_General.out"];

def pMap2 = [((Predicate)CandCat_CBL):dataroot+"NELL.part2.165.cesv.csv.Cand2Cat_CBL.out",
    	    ((Predicate)CandCat_CMC):dataroot+"NELL.part2.165.cesv.csv.Cand2Cat_CMC.out",
	    ((Predicate)CandCat_CPL):dataroot+"NELL.part2.165.cesv.csv.Cand2Cat_CPL.out",
	    ((Predicate)CandCat_General):dataroot+"NELL.part2.165.cesv.csv.Cand2Cat_General.out",
	    ((Predicate)CandCat_Morph):dataroot+"NELL.part2.165.cesv.csv.Cand2Cat_Morph.out",
	    ((Predicate)CandCat_SEAL):dataroot+"NELL.part2.165.cesv.csv.Cand2Cat_SEAL.out",
	    ((Predicate)CandRel_CBL):dataroot+"NELL.part2.165.cesv.csv.Cand2Rel_CBL.out",
	    ((Predicate)CandRel_CPL):dataroot+"NELL.part2.165.cesv.csv.Cand2Rel_CPL.out",
	    ((Predicate)CandRel_General):dataroot+"NELL.part2.165.cesv.csv.Cand2Rel_General.out",
	    ((Predicate)CandRel_SEAL):dataroot+"NELL.part2.165.cesv.csv.Cand2Rel_SEAL.out",
	    ((Predicate)PromCat_General):dataroot+"NELL.part2.165.esv.csv.Prom2Cat_General.out",
	    ((Predicate)PromRel_General):dataroot+"NELL.part2.165.esv.csv.Prom2Rel_General.out"];

def pMap3 = [((Predicate)CandCat_CBL):dataroot+"NELL.part3.165.cesv.csv.Cand3Cat_CBL.out",
    	    ((Predicate)CandCat_CMC):dataroot+"NELL.part3.165.cesv.csv.Cand3Cat_CMC.out",
	    ((Predicate)CandCat_CPL):dataroot+"NELL.part3.165.cesv.csv.Cand3Cat_CPL.out",
	    ((Predicate)CandCat_General):dataroot+"NELL.part3.165.cesv.csv.Cand3Cat_General.out",
	    ((Predicate)CandCat_Morph):dataroot+"NELL.part3.165.cesv.csv.Cand3Cat_Morph.out",
	    ((Predicate)CandCat_SEAL):dataroot+"NELL.part3.165.cesv.csv.Cand3Cat_SEAL.out",
	    ((Predicate)CandRel_CBL):dataroot+"NELL.part3.165.cesv.csv.Cand3Rel_CBL.out",
	    ((Predicate)CandRel_CPL):dataroot+"NELL.part3.165.cesv.csv.Cand3Rel_CPL.out",
	    ((Predicate)CandRel_General):dataroot+"NELL.part3.165.cesv.csv.Cand3Rel_General.out",
	    ((Predicate)CandRel_SEAL):dataroot+"NELL.part3.165.cesv.csv.Cand3Rel_SEAL.out",
	    ((Predicate)PromCat_General):dataroot+"NELL.part3.165.esv.csv.Prom3Cat_General.out",
	    ((Predicate)PromRel_General):dataroot+"NELL.part3.165.esv.csv.Prom3Rel_General.out"];

def pMap4 = [((Predicate)CandCat_CBL):dataroot+"NELL.part4.165.cesv.csv.Cand4Cat_CBL.out",
    	    ((Predicate)CandCat_CMC):dataroot+"NELL.part4.165.cesv.csv.Cand4Cat_CMC.out",
	    ((Predicate)CandCat_CPL):dataroot+"NELL.part4.165.cesv.csv.Cand4Cat_CPL.out",
	    ((Predicate)CandCat_General):dataroot+"NELL.part4.165.cesv.csv.Cand4Cat_General.out",
	    ((Predicate)CandCat_Morph):dataroot+"NELL.part4.165.cesv.csv.Cand4Cat_Morph.out",
	    ((Predicate)CandCat_SEAL):dataroot+"NELL.part4.165.cesv.csv.Cand4Cat_SEAL.out",
	    ((Predicate)CandRel_CBL):dataroot+"NELL.part4.165.cesv.csv.Cand4Rel_CBL.out",
	    ((Predicate)CandRel_CPL):dataroot+"NELL.part4.165.cesv.csv.Cand4Rel_CPL.out",
	    ((Predicate)CandRel_General):dataroot+"NELL.part4.165.cesv.csv.Cand4Rel_General.out",
	    ((Predicate)CandRel_SEAL):dataroot+"NELL.part4.165.cesv.csv.Cand4Rel_SEAL.out",
	    ((Predicate)PromCat_General):dataroot+"NELL.part4.165.esv.csv.Prom4Cat_General.out",
	    ((Predicate)PromRel_General):dataroot+"NELL.part4.165.esv.csv.Prom4Rel_General.out"];

def pMap5 = [((Predicate)CandCat_CBL):dataroot+"NELL.part5.165.cesv.csv.Cand5Cat_CBL.out",
    	    ((Predicate)CandCat_CMC):dataroot+"NELL.part5.165.cesv.csv.Cand5Cat_CMC.out",
	    ((Predicate)CandCat_CPL):dataroot+"NELL.part5.165.cesv.csv.Cand5Cat_CPL.out",
	    ((Predicate)CandCat_General):dataroot+"NELL.part5.165.cesv.csv.Cand5Cat_General.out",
	    ((Predicate)CandCat_Morph):dataroot+"NELL.part5.165.cesv.csv.Cand5Cat_Morph.out",
	    ((Predicate)CandCat_SEAL):dataroot+"NELL.part5.165.cesv.csv.Cand5Cat_SEAL.out",
	    ((Predicate)CandRel_CBL):dataroot+"NELL.part5.165.cesv.csv.Cand5Rel_CBL.out",
	    ((Predicate)CandRel_CPL):dataroot+"NELL.part5.165.cesv.csv.Cand5Rel_CPL.out",
	    ((Predicate)CandRel_General):dataroot+"NELL.part5.165.cesv.csv.Cand5Rel_General.out",
	    ((Predicate)CandRel_SEAL):dataroot+"NELL.part5.165.cesv.csv.Cand5Rel_SEAL.out",
	    ((Predicate)PromCat_General):dataroot+"NELL.part5.165.esv.csv.Prom5Cat_General.out",
	    ((Predicate)PromRel_General):dataroot+"NELL.part5.165.esv.csv.Prom5Rel_General.out"];

def pMap012 = [((Predicate)CandCat_CBL):dataroot+"NELL.part012.165.cesv.csv.Cand012Cat_CBL.out",
	       ((Predicate)CandCat_CMC):dataroot+"NELL.part012.165.cesv.csv.Cand012Cat_CMC.out",
	       ((Predicate)CandCat_CPL):dataroot+"NELL.part012.165.cesv.csv.Cand012Cat_CPL.out",
	       ((Predicate)CandCat_General):dataroot+"NELL.part012.165.cesv.csv.Cand012Cat_General.out",
	       ((Predicate)CandCat_Morph):dataroot+"NELL.part012.165.cesv.csv.Cand012Cat_Morph.out",
	       ((Predicate)CandCat_SEAL):dataroot+"NELL.part012.165.cesv.csv.Cand012Cat_SEAL.out",
	       ((Predicate)CandRel_CBL):dataroot+"NELL.part012.165.cesv.csv.Cand012Rel_CBL.out",
	       ((Predicate)CandRel_CPL):dataroot+"NELL.part012.165.cesv.csv.Cand012Rel_CPL.out",
	       ((Predicate)CandRel_General):dataroot+"NELL.part012.165.cesv.csv.Cand012Rel_General.out",
	       ((Predicate)CandRel_SEAL):dataroot+"NELL.part012.165.cesv.csv.Cand012Rel_SEAL.out",
	       ((Predicate)PromCat_General):dataroot+"NELL.part012.165.esv.csv.Prom012Cat_General.out",
	       ((Predicate)PromRel_General):dataroot+"NELL.part012.165.esv.csv.Prom012Rel_General.out"];

pMap345 = [((Predicate)CandCat_CBL):dataroot+"NELL.part345.165.cesv.csv.Cand345Cat_CBL.out",
       	 ((Predicate)CandCat_CMC):dataroot+"NELL.part345.165.cesv.csv.Cand345Cat_CMC.out",
	 ((Predicate)CandCat_CPL):dataroot+"NELL.part345.165.cesv.csv.Cand345Cat_CPL.out",
	 ((Predicate)CandCat_General):dataroot+"NELL.part345.165.cesv.csv.Cand345Cat_General.out",
	 ((Predicate)CandCat_Morph):dataroot+"NELL.part345.165.cesv.csv.Cand345Cat_Morph.out",
	 ((Predicate)CandCat_SEAL):dataroot+"NELL.part345.165.cesv.csv.Cand345Cat_SEAL.out",
	 ((Predicate)CandRel_CBL):dataroot+"NELL.part345.165.cesv.csv.Cand345Rel_CBL.out",
	 ((Predicate)CandRel_CPL):dataroot+"NELL.part345.165.cesv.csv.Cand345Rel_CPL.out",
	 ((Predicate)CandRel_General):dataroot+"NELL.part345.165.cesv.csv.Cand345Rel_General.out",
	 ((Predicate)CandRel_SEAL):dataroot+"NELL.part345.165.cesv.csv.Cand345Rel_SEAL.out",
	 ((Predicate)PromCat_General):dataroot+"NELL.part345.165.esv.csv.Prom345Cat_General.out",
	 ((Predicate)PromRel_General):dataroot+"NELL.part345.165.esv.csv.Prom345Rel_General.out"];


pMap01 = [((Predicate)CandCat_CBL):dataroot+"NELL.part01.165.cesv.csv.Cand01Cat_CBL.out",
       	 ((Predicate)CandCat_CMC):dataroot+"NELL.part01.165.cesv.csv.Cand01Cat_CMC.out",
	 ((Predicate)CandCat_CPL):dataroot+"NELL.part01.165.cesv.csv.Cand01Cat_CPL.out",
	 ((Predicate)CandCat_General):dataroot+"NELL.part01.165.cesv.csv.Cand01Cat_General.out",
	 ((Predicate)CandCat_Morph):dataroot+"NELL.part01.165.cesv.csv.Cand01Cat_Morph.out",
	 ((Predicate)CandCat_SEAL):dataroot+"NELL.part01.165.cesv.csv.Cand01Cat_SEAL.out",
	 ((Predicate)CandRel_CBL):dataroot+"NELL.part01.165.cesv.csv.Cand01Rel_CBL.out",
	 ((Predicate)CandRel_CPL):dataroot+"NELL.part01.165.cesv.csv.Cand01Rel_CPL.out",
	 ((Predicate)CandRel_General):dataroot+"NELL.part01.165.cesv.csv.Cand01Rel_General.out",
	 ((Predicate)CandRel_SEAL):dataroot+"NELL.part01.165.cesv.csv.Cand01Rel_SEAL.out",
	 ((Predicate)PromCat_General):dataroot+"NELL.part01.165.esv.csv.Prom01Cat_General.out",
	 ((Predicate)PromRel_General):dataroot+"NELL.part01.165.esv.csv.Prom01Rel_General.out"];

pMap23 = [((Predicate)CandCat_CBL):dataroot+"NELL.part23.165.cesv.csv.Cand23Cat_CBL.out",
       	 ((Predicate)CandCat_CMC):dataroot+"NELL.part23.165.cesv.csv.Cand23Cat_CMC.out",
	 ((Predicate)CandCat_CPL):dataroot+"NELL.part23.165.cesv.csv.Cand23Cat_CPL.out",
	 ((Predicate)CandCat_General):dataroot+"NELL.part23.165.cesv.csv.Cand23Cat_General.out",
	 ((Predicate)CandCat_Morph):dataroot+"NELL.part23.165.cesv.csv.Cand23Cat_Morph.out",
	 ((Predicate)CandCat_SEAL):dataroot+"NELL.part23.165.cesv.csv.Cand23Cat_SEAL.out",
	 ((Predicate)CandRel_CBL):dataroot+"NELL.part23.165.cesv.csv.Cand23Rel_CBL.out",
	 ((Predicate)CandRel_CPL):dataroot+"NELL.part23.165.cesv.csv.Cand23Rel_CPL.out",
	 ((Predicate)CandRel_General):dataroot+"NELL.part23.165.cesv.csv.Cand23Rel_General.out",
	 ((Predicate)CandRel_SEAL):dataroot+"NELL.part23.165.cesv.csv.Cand23Rel_SEAL.out",
	 ((Predicate)PromCat_General):dataroot+"NELL.part23.165.esv.csv.Prom23Cat_General.out",
	 ((Predicate)PromRel_General):dataroot+"NELL.part23.165.esv.csv.Prom23Rel_General.out"];

pMap45 = [((Predicate)CandCat_CBL):dataroot+"NELL.part45.165.cesv.csv.Cand45Cat_CBL.out",
       	 ((Predicate)CandCat_CMC):dataroot+"NELL.part45.165.cesv.csv.Cand45Cat_CMC.out",
       	 ((Predicate)CandCat_CPL):dataroot+"NELL.part45.165.cesv.csv.Cand45Cat_CPL.out",
	 ((Predicate)CandCat_General):dataroot+"NELL.part45.165.cesv.csv.Cand45Cat_General.out",
	 ((Predicate)CandCat_Morph):dataroot+"NELL.part45.165.cesv.csv.Cand45Cat_Morph.out",
	 ((Predicate)CandCat_SEAL):dataroot+"NELL.part45.165.cesv.csv.Cand45Cat_SEAL.out",
	 ((Predicate)CandRel_CBL):dataroot+"NELL.part45.165.cesv.csv.Cand45Rel_CBL.out",
	 ((Predicate)CandRel_CPL):dataroot+"NELL.part45.165.cesv.csv.Cand45Rel_CPL.out",
	 ((Predicate)CandRel_General):dataroot+"NELL.part45.165.cesv.csv.Cand45Rel_General.out",
	 ((Predicate)CandRel_SEAL):dataroot+"NELL.part45.165.cesv.csv.Cand45Rel_SEAL.out",
	 ((Predicate)PromCat_General):dataroot+"NELL.part45.165.esv.csv.Prom45Cat_General.out",
	 ((Predicate)PromRel_General):dataroot+"NELL.part45.165.esv.csv.Prom45Rel_General.out"];


pMaps = [((Map)pMap0):facts0, ((Map)pMap1):facts1, ((Map)pMap2):facts2, ((Map)pMap3):facts3, ((Map)pMap4):facts4, ((Map)pMap5):facts5, ((Map)pMap012345):facts012345, ((Map)pMap012):facts012, ((Map)pMap345):facts345, ((Map)pMap01):facts01, ((Map)pMap23):facts23, ((Map)pMap45):facts45];
for( Map factMap : pMaps.keySet()){
   for (Predicate p : factMap.keySet() ){
        insert = data.getInserter(p,pMaps[factMap]);
    	java.io.File f = new File(factMap[p]);
	if(f.exists()){
	    log.info("Loading files "+factMap[p]);
	    InserterUtils.loadDelimitedDataTruth(insert,factMap[p]);
	} else {
	    log.warn("Warning!  No file "+factMap[p]);
	}
   }
}

Partition training = new Partition(22);

insert = data.getInserter(Cat, training);
log.info("Loading files " + dataroot + "label-train-uniq-raw-cat.db.TRAIN");
InserterUtils.loadDelimitedDataTruth(insert, dataroot + "label-train-uniq-raw-cat.db");

insert = data.getInserter(Rel, training);
log.info("Loading files " + dataroot + "label-train-uniq-raw-rel.db.TRAIN");
InserterUtils.loadDelimitedDataTruth(insert, dataroot + "label-train-uniq-raw-rel.db");

log.info("data loading finished")

Date stop = new Date();

TimeDuration td = TimeCategory.minus( stop, start );
log.info("Total loading time"+td);
