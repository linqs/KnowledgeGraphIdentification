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
System.out.println "[info] \t\tDECLARING PREDICATES...";

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
//Partition entityResolution = new Partition(11);


//def dataroot = "data/part3/";
def dataroot = args[0];

def ontoMap = [
		((Predicate)Mut):dataroot+"onto-wbpg.db.Mut.txt",
		((Predicate)Sub):dataroot+"onto-wbpg.db.Sub.txt",
		((Predicate)RSub):dataroot+"onto-wbpg.db.RSub.txt",
    	      	((Predicate)Domain):dataroot+"onto-wbpg.db.Domain.txt",
		((Predicate)Inv):dataroot+"onto-wbpg.db.Inv.txt",
		((Predicate)Range2):dataroot+"onto-wbpg.db.Range2.txt",
		((Predicate)RMut):dataroot+"onto-wbpg.db.RMut.txt"];

// load constrain cat ontology to 11
def pMap = ontoMap;

for (Predicate p : pMap.keySet() ){
    insert = data.getInserter(p,ontology);
    System.out.println("Loading files "+pMap[p]);
    InserterUtils.loadDelimitedData(insert,pMap[p]);
}


// load the seed data
Partition seeds = new Partition(40);

insert = data.getInserter(Cat, seeds);
System.out.println("Loading files seed.165.cat.uniq.out");
InserterUtils.loadDelimitedDataTruth(insert,dataroot+"seed-conv-raw-cat.db")
insert = data.getInserter(Rel, seeds);
System.out.println("Loading files seed.165.rel.uniq.out");
InserterUtils.loadDelimitedDataTruth(insert,dataroot+"seed-conv-raw-rel.db")

// other partitions
Partition facts0 = new Partition(100);
Partition facts1 = new Partition(101);
Partition facts2 = new Partition(102);
Partition facts3 = new Partition(103);
Partition facts4 = new Partition(104);
Partition facts5 = new Partition(105);
Partition facts6 = new Partition(106);
Partition facts7 = new Partition(107);
Partition facts8 = new Partition(108);
Partition facts9 = new Partition(109);
Partition facts10 = new Partition(110);
Partition facts11 = new Partition(111);
Partition facts12 = new Partition(112);
Partition facts13 = new Partition(113);
Partition facts14 = new Partition(114);
Partition facts15 = new Partition(115);
Partition facts16 = new Partition(116);
Partition facts17 = new Partition(117);
Partition facts18 = new Partition(118);
Partition facts19 = new Partition(119);
Partition facts20 = new Partition(120);
Partition facts21 = new Partition(121);
Partition facts22 = new Partition(122);
Partition facts23 = new Partition(123);
Partition facts24 = new Partition(124);
Partition facts25 = new Partition(125);
Partition facts26 = new Partition(126);
Partition facts27 = new Partition(127);
Partition facts28 = new Partition(128);
Partition facts29 = new Partition(129);
Partition facts30 = new Partition(130);
Partition facts31 = new Partition(131);
Partition facts32 = new Partition(132);
Partition facts33 = new Partition(133);
Partition facts34 = new Partition(134);
Partition facts35 = new Partition(135);
Partition facts36 = new Partition(136);
Partition facts37 = new Partition(137);
Partition facts38 = new Partition(138);
Partition facts39 = new Partition(139);
Partition facts40 = new Partition(140);
Partition facts41 = new Partition(141);
Partition facts42 = new Partition(142);
Partition facts43 = new Partition(143);
Partition facts44 = new Partition(144);
Partition facts45 = new Partition(145);
Partition facts46 = new Partition(146);
Partition facts47 = new Partition(147);




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

def pMap6 = [((Predicate)CandCat_CBL):dataroot+"NELL.part6.165.cesv.csv.Cand6Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part6.165.cesv.csv.Cand6Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part6.165.cesv.csv.Cand6Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part6.165.cesv.csv.Cand6Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part6.165.cesv.csv.Cand6Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part6.165.cesv.csv.Cand6Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part6.165.cesv.csv.Cand6Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part6.165.cesv.csv.Cand6Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part6.165.cesv.csv.Cand6Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part6.165.cesv.csv.Cand6Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part6.165.esv.csv.Prom6Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part6.165.esv.csv.Prom6Rel_General.out"];

def pMap7 = [((Predicate)CandCat_CBL):dataroot+"NELL.part7.165.cesv.csv.Cand7Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part7.165.cesv.csv.Cand7Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part7.165.cesv.csv.Cand7Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part7.165.cesv.csv.Cand7Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part7.165.cesv.csv.Cand7Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part7.165.cesv.csv.Cand7Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part7.165.cesv.csv.Cand7Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part7.165.cesv.csv.Cand7Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part7.165.cesv.csv.Cand7Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part7.165.cesv.csv.Cand7Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part7.165.esv.csv.Prom7Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part7.165.esv.csv.Prom7Rel_General.out"];

def pMap8 = [((Predicate)CandCat_CBL):dataroot+"NELL.part8.165.cesv.csv.Cand8Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part8.165.cesv.csv.Cand8Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part8.165.cesv.csv.Cand8Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part8.165.cesv.csv.Cand8Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part8.165.cesv.csv.Cand8Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part8.165.cesv.csv.Cand8Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part8.165.cesv.csv.Cand8Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part8.165.cesv.csv.Cand8Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part8.165.cesv.csv.Cand8Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part8.165.cesv.csv.Cand8Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part8.165.esv.csv.Prom8Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part8.165.esv.csv.Prom8Rel_General.out"];

def pMap9 = [((Predicate)CandCat_CBL):dataroot+"NELL.part9.165.cesv.csv.Cand9Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part9.165.cesv.csv.Cand9Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part9.165.cesv.csv.Cand9Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part9.165.cesv.csv.Cand9Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part9.165.cesv.csv.Cand9Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part9.165.cesv.csv.Cand9Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part9.165.cesv.csv.Cand9Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part9.165.cesv.csv.Cand9Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part9.165.cesv.csv.Cand9Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part9.165.cesv.csv.Cand9Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part9.165.esv.csv.Prom9Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part9.165.esv.csv.Prom9Rel_General.out"];

def pMap10 = [((Predicate)CandCat_CBL):dataroot+"NELL.part10.165.cesv.csv.Cand10Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part10.165.cesv.csv.Cand10Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part10.165.cesv.csv.Cand10Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part10.165.cesv.csv.Cand10Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part10.165.cesv.csv.Cand10Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part10.165.cesv.csv.Cand10Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part10.165.cesv.csv.Cand10Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part10.165.cesv.csv.Cand10Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part10.165.cesv.csv.Cand10Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part10.165.cesv.csv.Cand10Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part10.165.esv.csv.Prom10Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part10.165.esv.csv.Prom10Rel_General.out"];

def pMap11 = [((Predicate)CandCat_CBL):dataroot+"NELL.part11.165.cesv.csv.Cand11Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part11.165.cesv.csv.Cand11Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part11.165.cesv.csv.Cand11Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part11.165.cesv.csv.Cand11Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part11.165.cesv.csv.Cand11Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part11.165.cesv.csv.Cand11Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part11.165.cesv.csv.Cand11Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part11.165.cesv.csv.Cand11Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part11.165.cesv.csv.Cand11Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part11.165.cesv.csv.Cand11Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part11.165.esv.csv.Prom11Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part11.165.esv.csv.Prom11Rel_General.out"];

def pMap12 = [((Predicate)CandCat_CBL):dataroot+"NELL.part12.165.cesv.csv.Cand12Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part12.165.cesv.csv.Cand12Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part12.165.cesv.csv.Cand12Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part12.165.cesv.csv.Cand12Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part12.165.cesv.csv.Cand12Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part12.165.cesv.csv.Cand12Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part12.165.cesv.csv.Cand12Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part12.165.cesv.csv.Cand12Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part12.165.cesv.csv.Cand12Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part12.165.cesv.csv.Cand12Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part12.165.esv.csv.Prom12Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part12.165.esv.csv.Prom12Rel_General.out"];

def pMap13 = [((Predicate)CandCat_CBL):dataroot+"NELL.part13.165.cesv.csv.Cand13Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part13.165.cesv.csv.Cand13Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part13.165.cesv.csv.Cand13Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part13.165.cesv.csv.Cand13Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part13.165.cesv.csv.Cand13Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part13.165.cesv.csv.Cand13Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part13.165.cesv.csv.Cand13Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part13.165.cesv.csv.Cand13Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part13.165.cesv.csv.Cand13Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part13.165.cesv.csv.Cand13Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part13.165.esv.csv.Prom13Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part13.165.esv.csv.Prom13Rel_General.out"];

def pMap14 = [((Predicate)CandCat_CBL):dataroot+"NELL.part14.165.cesv.csv.Cand14Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part14.165.cesv.csv.Cand14Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part14.165.cesv.csv.Cand14Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part14.165.cesv.csv.Cand14Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part14.165.cesv.csv.Cand14Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part14.165.cesv.csv.Cand14Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part14.165.cesv.csv.Cand14Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part14.165.cesv.csv.Cand14Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part14.165.cesv.csv.Cand14Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part14.165.cesv.csv.Cand14Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part14.165.esv.csv.Prom14Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part14.165.esv.csv.Prom14Rel_General.out"];

def pMap15 = [((Predicate)CandCat_CBL):dataroot+"NELL.part15.165.cesv.csv.Cand15Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part15.165.cesv.csv.Cand15Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part15.165.cesv.csv.Cand15Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part15.165.cesv.csv.Cand15Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part15.165.cesv.csv.Cand15Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part15.165.cesv.csv.Cand15Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part15.165.cesv.csv.Cand15Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part15.165.cesv.csv.Cand15Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part15.165.cesv.csv.Cand15Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part15.165.cesv.csv.Cand15Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part15.165.esv.csv.Prom15Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part15.165.esv.csv.Prom15Rel_General.out"];

def pMap16 = [((Predicate)CandCat_CBL):dataroot+"NELL.part16.165.cesv.csv.Cand16Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part16.165.cesv.csv.Cand16Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part16.165.cesv.csv.Cand16Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part16.165.cesv.csv.Cand16Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part16.165.cesv.csv.Cand16Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part16.165.cesv.csv.Cand16Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part16.165.cesv.csv.Cand16Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part16.165.cesv.csv.Cand16Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part16.165.cesv.csv.Cand16Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part16.165.cesv.csv.Cand16Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part16.165.esv.csv.Prom16Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part16.165.esv.csv.Prom16Rel_General.out"];

def pMap17 = [((Predicate)CandCat_CBL):dataroot+"NELL.part17.165.cesv.csv.Cand17Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part17.165.cesv.csv.Cand17Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part17.165.cesv.csv.Cand17Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part17.165.cesv.csv.Cand17Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part17.165.cesv.csv.Cand17Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part17.165.cesv.csv.Cand17Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part17.165.cesv.csv.Cand17Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part17.165.cesv.csv.Cand17Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part17.165.cesv.csv.Cand17Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part17.165.cesv.csv.Cand17Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part17.165.esv.csv.Prom17Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part17.165.esv.csv.Prom17Rel_General.out"];

def pMap18 = [((Predicate)CandCat_CBL):dataroot+"NELL.part18.165.cesv.csv.Cand18Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part18.165.cesv.csv.Cand18Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part18.165.cesv.csv.Cand18Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part18.165.cesv.csv.Cand18Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part18.165.cesv.csv.Cand18Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part18.165.cesv.csv.Cand18Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part18.165.cesv.csv.Cand18Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part18.165.cesv.csv.Cand18Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part18.165.cesv.csv.Cand18Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part18.165.cesv.csv.Cand18Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part18.165.esv.csv.Prom18Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part18.165.esv.csv.Prom18Rel_General.out"];

def pMap19 = [((Predicate)CandCat_CBL):dataroot+"NELL.part19.165.cesv.csv.Cand19Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part19.165.cesv.csv.Cand19Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part19.165.cesv.csv.Cand19Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part19.165.cesv.csv.Cand19Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part19.165.cesv.csv.Cand19Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part19.165.cesv.csv.Cand19Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part19.165.cesv.csv.Cand19Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part19.165.cesv.csv.Cand19Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part19.165.cesv.csv.Cand19Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part19.165.cesv.csv.Cand19Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part19.165.esv.csv.Prom19Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part19.165.esv.csv.Prom19Rel_General.out"];

def pMap20 = [((Predicate)CandCat_CBL):dataroot+"NELL.part20.165.cesv.csv.Cand20Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part20.165.cesv.csv.Cand20Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part20.165.cesv.csv.Cand20Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part20.165.cesv.csv.Cand20Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part20.165.cesv.csv.Cand20Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part20.165.cesv.csv.Cand20Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part20.165.cesv.csv.Cand20Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part20.165.cesv.csv.Cand20Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part20.165.cesv.csv.Cand20Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part20.165.cesv.csv.Cand20Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part20.165.esv.csv.Prom20Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part20.165.esv.csv.Prom20Rel_General.out"];

def pMap21 = [((Predicate)CandCat_CBL):dataroot+"NELL.part21.165.cesv.csv.Cand21Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part21.165.cesv.csv.Cand21Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part21.165.cesv.csv.Cand21Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part21.165.cesv.csv.Cand21Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part21.165.cesv.csv.Cand21Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part21.165.cesv.csv.Cand21Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part21.165.cesv.csv.Cand21Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part21.165.cesv.csv.Cand21Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part21.165.cesv.csv.Cand21Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part21.165.cesv.csv.Cand21Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part21.165.esv.csv.Prom21Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part21.165.esv.csv.Prom21Rel_General.out"];

def pMap22 = [((Predicate)CandCat_CBL):dataroot+"NELL.part22.165.cesv.csv.Cand22Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part22.165.cesv.csv.Cand22Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part22.165.cesv.csv.Cand22Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part22.165.cesv.csv.Cand22Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part22.165.cesv.csv.Cand22Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part22.165.cesv.csv.Cand22Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part22.165.cesv.csv.Cand22Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part22.165.cesv.csv.Cand22Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part22.165.cesv.csv.Cand22Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part22.165.cesv.csv.Cand22Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part22.165.esv.csv.Prom22Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part22.165.esv.csv.Prom22Rel_General.out"];

def pMap23 = [((Predicate)CandCat_CBL):dataroot+"NELL.part23.165.cesv.csv.Cand23Cat_CBL.out",
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

def pMap24 = [((Predicate)CandCat_CBL):dataroot+"NELL.part24.165.cesv.csv.Cand24Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part24.165.cesv.csv.Cand24Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part24.165.cesv.csv.Cand24Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part24.165.cesv.csv.Cand24Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part24.165.cesv.csv.Cand24Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part24.165.cesv.csv.Cand24Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part24.165.cesv.csv.Cand24Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part24.165.cesv.csv.Cand24Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part24.165.cesv.csv.Cand24Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part24.165.cesv.csv.Cand24Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part24.165.esv.csv.Prom24Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part24.165.esv.csv.Prom24Rel_General.out"];

def pMap25 = [((Predicate)CandCat_CBL):dataroot+"NELL.part25.165.cesv.csv.Cand25Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part25.165.cesv.csv.Cand25Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part25.165.cesv.csv.Cand25Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part25.165.cesv.csv.Cand25Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part25.165.cesv.csv.Cand25Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part25.165.cesv.csv.Cand25Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part25.165.cesv.csv.Cand25Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part25.165.cesv.csv.Cand25Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part25.165.cesv.csv.Cand25Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part25.165.cesv.csv.Cand25Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part25.165.esv.csv.Prom25Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part25.165.esv.csv.Prom25Rel_General.out"];

def pMap26 = [((Predicate)CandCat_CBL):dataroot+"NELL.part26.165.cesv.csv.Cand26Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part26.165.cesv.csv.Cand26Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part26.165.cesv.csv.Cand26Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part26.165.cesv.csv.Cand26Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part26.165.cesv.csv.Cand26Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part26.165.cesv.csv.Cand26Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part26.165.cesv.csv.Cand26Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part26.165.cesv.csv.Cand26Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part26.165.cesv.csv.Cand26Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part26.165.cesv.csv.Cand26Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part26.165.esv.csv.Prom26Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part26.165.esv.csv.Prom26Rel_General.out"];

def pMap27 = [((Predicate)CandCat_CBL):dataroot+"NELL.part27.165.cesv.csv.Cand27Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part27.165.cesv.csv.Cand27Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part27.165.cesv.csv.Cand27Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part27.165.cesv.csv.Cand27Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part27.165.cesv.csv.Cand27Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part27.165.cesv.csv.Cand27Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part27.165.cesv.csv.Cand27Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part27.165.cesv.csv.Cand27Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part27.165.cesv.csv.Cand27Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part27.165.cesv.csv.Cand27Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part27.165.esv.csv.Prom27Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part27.165.esv.csv.Prom27Rel_General.out"];

def pMap28 = [((Predicate)CandCat_CBL):dataroot+"NELL.part28.165.cesv.csv.Cand28Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part28.165.cesv.csv.Cand28Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part28.165.cesv.csv.Cand28Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part28.165.cesv.csv.Cand28Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part28.165.cesv.csv.Cand28Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part28.165.cesv.csv.Cand28Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part28.165.cesv.csv.Cand28Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part28.165.cesv.csv.Cand28Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part28.165.cesv.csv.Cand28Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part28.165.cesv.csv.Cand28Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part28.165.esv.csv.Prom28Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part28.165.esv.csv.Prom28Rel_General.out"];

def pMap29 = [((Predicate)CandCat_CBL):dataroot+"NELL.part29.165.cesv.csv.Cand29Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part29.165.cesv.csv.Cand29Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part29.165.cesv.csv.Cand29Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part29.165.cesv.csv.Cand29Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part29.165.cesv.csv.Cand29Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part29.165.cesv.csv.Cand29Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part29.165.cesv.csv.Cand29Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part29.165.cesv.csv.Cand29Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part29.165.cesv.csv.Cand29Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part29.165.cesv.csv.Cand29Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part29.165.esv.csv.Prom29Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part29.165.esv.csv.Prom29Rel_General.out"];

def pMap30 = [((Predicate)CandCat_CBL):dataroot+"NELL.part30.165.cesv.csv.Cand30Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part30.165.cesv.csv.Cand30Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part30.165.cesv.csv.Cand30Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part30.165.cesv.csv.Cand30Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part30.165.cesv.csv.Cand30Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part30.165.cesv.csv.Cand30Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part30.165.cesv.csv.Cand30Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part30.165.cesv.csv.Cand30Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part30.165.cesv.csv.Cand30Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part30.165.cesv.csv.Cand30Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part30.165.esv.csv.Prom30Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part30.165.esv.csv.Prom30Rel_General.out"];

def pMap31 = [((Predicate)CandCat_CBL):dataroot+"NELL.part31.165.cesv.csv.Cand31Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part31.165.cesv.csv.Cand31Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part31.165.cesv.csv.Cand31Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part31.165.cesv.csv.Cand31Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part31.165.cesv.csv.Cand31Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part31.165.cesv.csv.Cand31Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part31.165.cesv.csv.Cand31Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part31.165.cesv.csv.Cand31Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part31.165.cesv.csv.Cand31Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part31.165.cesv.csv.Cand31Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part31.165.esv.csv.Prom31Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part31.165.esv.csv.Prom31Rel_General.out"];

def pMap32 = [((Predicate)CandCat_CBL):dataroot+"NELL.part32.165.cesv.csv.Cand32Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part32.165.cesv.csv.Cand32Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part32.165.cesv.csv.Cand32Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part32.165.cesv.csv.Cand32Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part32.165.cesv.csv.Cand32Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part32.165.cesv.csv.Cand32Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part32.165.cesv.csv.Cand32Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part32.165.cesv.csv.Cand32Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part32.165.cesv.csv.Cand32Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part32.165.cesv.csv.Cand32Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part32.165.esv.csv.Prom32Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part32.165.esv.csv.Prom32Rel_General.out"];

def pMap33 = [((Predicate)CandCat_CBL):dataroot+"NELL.part33.165.cesv.csv.Cand33Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part33.165.cesv.csv.Cand33Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part33.165.cesv.csv.Cand33Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part33.165.cesv.csv.Cand33Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part33.165.cesv.csv.Cand33Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part33.165.cesv.csv.Cand33Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part33.165.cesv.csv.Cand33Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part33.165.cesv.csv.Cand33Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part33.165.cesv.csv.Cand33Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part33.165.cesv.csv.Cand33Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part33.165.esv.csv.Prom33Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part33.165.esv.csv.Prom33Rel_General.out"];

def pMap34 = [((Predicate)CandCat_CBL):dataroot+"NELL.part34.165.cesv.csv.Cand34Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part34.165.cesv.csv.Cand34Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part34.165.cesv.csv.Cand34Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part34.165.cesv.csv.Cand34Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part34.165.cesv.csv.Cand34Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part34.165.cesv.csv.Cand34Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part34.165.cesv.csv.Cand34Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part34.165.cesv.csv.Cand34Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part34.165.cesv.csv.Cand34Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part34.165.cesv.csv.Cand34Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part34.165.esv.csv.Prom34Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part34.165.esv.csv.Prom34Rel_General.out"];

def pMap35 = [((Predicate)CandCat_CBL):dataroot+"NELL.part35.165.cesv.csv.Cand35Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part35.165.cesv.csv.Cand35Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part35.165.cesv.csv.Cand35Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part35.165.cesv.csv.Cand35Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part35.165.cesv.csv.Cand35Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part35.165.cesv.csv.Cand35Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part35.165.cesv.csv.Cand35Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part35.165.cesv.csv.Cand35Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part35.165.cesv.csv.Cand35Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part35.165.cesv.csv.Cand35Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part35.165.esv.csv.Prom35Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part35.165.esv.csv.Prom35Rel_General.out"];

def pMap36 = [((Predicate)CandCat_CBL):dataroot+"NELL.part36.165.cesv.csv.Cand36Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part36.165.cesv.csv.Cand36Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part36.165.cesv.csv.Cand36Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part36.165.cesv.csv.Cand36Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part36.165.cesv.csv.Cand36Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part36.165.cesv.csv.Cand36Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part36.165.cesv.csv.Cand36Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part36.165.cesv.csv.Cand36Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part36.165.cesv.csv.Cand36Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part36.165.cesv.csv.Cand36Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part36.165.esv.csv.Prom36Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part36.165.esv.csv.Prom36Rel_General.out"];

def pMap37 = [((Predicate)CandCat_CBL):dataroot+"NELL.part37.165.cesv.csv.Cand37Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part37.165.cesv.csv.Cand37Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part37.165.cesv.csv.Cand37Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part37.165.cesv.csv.Cand37Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part37.165.cesv.csv.Cand37Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part37.165.cesv.csv.Cand37Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part37.165.cesv.csv.Cand37Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part37.165.cesv.csv.Cand37Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part37.165.cesv.csv.Cand37Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part37.165.cesv.csv.Cand37Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part37.165.esv.csv.Prom37Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part37.165.esv.csv.Prom37Rel_General.out"];

def pMap38 = [((Predicate)CandCat_CBL):dataroot+"NELL.part38.165.cesv.csv.Cand38Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part38.165.cesv.csv.Cand38Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part38.165.cesv.csv.Cand38Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part38.165.cesv.csv.Cand38Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part38.165.cesv.csv.Cand38Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part38.165.cesv.csv.Cand38Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part38.165.cesv.csv.Cand38Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part38.165.cesv.csv.Cand38Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part38.165.cesv.csv.Cand38Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part38.165.cesv.csv.Cand38Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part38.165.esv.csv.Prom38Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part38.165.esv.csv.Prom38Rel_General.out"];

def pMap39 = [((Predicate)CandCat_CBL):dataroot+"NELL.part39.165.cesv.csv.Cand39Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part39.165.cesv.csv.Cand39Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part39.165.cesv.csv.Cand39Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part39.165.cesv.csv.Cand39Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part39.165.cesv.csv.Cand39Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part39.165.cesv.csv.Cand39Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part39.165.cesv.csv.Cand39Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part39.165.cesv.csv.Cand39Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part39.165.cesv.csv.Cand39Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part39.165.cesv.csv.Cand39Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part39.165.esv.csv.Prom39Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part39.165.esv.csv.Prom39Rel_General.out"];

def pMap40 = [((Predicate)CandCat_CBL):dataroot+"NELL.part40.165.cesv.csv.Cand40Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part40.165.cesv.csv.Cand40Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part40.165.cesv.csv.Cand40Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part40.165.cesv.csv.Cand40Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part40.165.cesv.csv.Cand40Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part40.165.cesv.csv.Cand40Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part40.165.cesv.csv.Cand40Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part40.165.cesv.csv.Cand40Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part40.165.cesv.csv.Cand40Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part40.165.cesv.csv.Cand40Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part40.165.esv.csv.Prom40Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part40.165.esv.csv.Prom40Rel_General.out"];

def pMap41 = [((Predicate)CandCat_CBL):dataroot+"NELL.part41.165.cesv.csv.Cand41Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part41.165.cesv.csv.Cand41Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part41.165.cesv.csv.Cand41Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part41.165.cesv.csv.Cand41Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part41.165.cesv.csv.Cand41Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part41.165.cesv.csv.Cand41Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part41.165.cesv.csv.Cand41Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part41.165.cesv.csv.Cand41Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part41.165.cesv.csv.Cand41Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part41.165.cesv.csv.Cand41Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part41.165.esv.csv.Prom41Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part41.165.esv.csv.Prom41Rel_General.out"];

def pMap42 = [((Predicate)CandCat_CBL):dataroot+"NELL.part42.165.cesv.csv.Cand42Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part42.165.cesv.csv.Cand42Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part42.165.cesv.csv.Cand42Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part42.165.cesv.csv.Cand42Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part42.165.cesv.csv.Cand42Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part42.165.cesv.csv.Cand42Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part42.165.cesv.csv.Cand42Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part42.165.cesv.csv.Cand42Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part42.165.cesv.csv.Cand42Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part42.165.cesv.csv.Cand42Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part42.165.esv.csv.Prom42Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part42.165.esv.csv.Prom42Rel_General.out"];

def pMap43 = [((Predicate)CandCat_CBL):dataroot+"NELL.part43.165.cesv.csv.Cand43Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part43.165.cesv.csv.Cand43Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part43.165.cesv.csv.Cand43Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part43.165.cesv.csv.Cand43Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part43.165.cesv.csv.Cand43Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part43.165.cesv.csv.Cand43Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part43.165.cesv.csv.Cand43Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part43.165.cesv.csv.Cand43Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part43.165.cesv.csv.Cand43Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part43.165.cesv.csv.Cand43Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part43.165.esv.csv.Prom43Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part43.165.esv.csv.Prom43Rel_General.out"];

def pMap44 = [((Predicate)CandCat_CBL):dataroot+"NELL.part44.165.cesv.csv.Cand44Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part44.165.cesv.csv.Cand44Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part44.165.cesv.csv.Cand44Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part44.165.cesv.csv.Cand44Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part44.165.cesv.csv.Cand44Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part44.165.cesv.csv.Cand44Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part44.165.cesv.csv.Cand44Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part44.165.cesv.csv.Cand44Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part44.165.cesv.csv.Cand44Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part44.165.cesv.csv.Cand44Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part44.165.esv.csv.Prom44Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part44.165.esv.csv.Prom44Rel_General.out"];

def pMap45 = [((Predicate)CandCat_CBL):dataroot+"NELL.part45.165.cesv.csv.Cand45Cat_CBL.out",
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

def pMap46 = [((Predicate)CandCat_CBL):dataroot+"NELL.part46.165.cesv.csv.Cand46Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part46.165.cesv.csv.Cand46Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part46.165.cesv.csv.Cand46Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part46.165.cesv.csv.Cand46Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part46.165.cesv.csv.Cand46Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part46.165.cesv.csv.Cand46Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part46.165.cesv.csv.Cand46Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part46.165.cesv.csv.Cand46Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part46.165.cesv.csv.Cand46Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part46.165.cesv.csv.Cand46Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part46.165.esv.csv.Prom46Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part46.165.esv.csv.Prom46Rel_General.out"];

def pMap47 = [((Predicate)CandCat_CBL):dataroot+"NELL.part47.165.cesv.csv.Cand47Cat_CBL.out",
            ((Predicate)CandCat_CMC):dataroot+"NELL.part47.165.cesv.csv.Cand47Cat_CMC.out",
            ((Predicate)CandCat_CPL):dataroot+"NELL.part47.165.cesv.csv.Cand47Cat_CPL.out",
            ((Predicate)CandCat_General):dataroot+"NELL.part47.165.cesv.csv.Cand47Cat_General.out",
            ((Predicate)CandCat_Morph):dataroot+"NELL.part47.165.cesv.csv.Cand47Cat_Morph.out",
            ((Predicate)CandCat_SEAL):dataroot+"NELL.part47.165.cesv.csv.Cand47Cat_SEAL.out",
            ((Predicate)CandRel_CBL):dataroot+"NELL.part47.165.cesv.csv.Cand47Rel_CBL.out",
            ((Predicate)CandRel_CPL):dataroot+"NELL.part47.165.cesv.csv.Cand47Rel_CPL.out",
            ((Predicate)CandRel_General):dataroot+"NELL.part47.165.cesv.csv.Cand47Rel_General.out",
            ((Predicate)CandRel_SEAL):dataroot+"NELL.part47.165.cesv.csv.Cand47Rel_SEAL.out",
            ((Predicate)PromCat_General):dataroot+"NELL.part47.165.esv.csv.Prom47Cat_General.out",
	     ((Predicate)PromRel_General):dataroot+"NELL.part47.165.esv.csv.Prom47Rel_General.out"];


/**/
pMaps = [((Map)pMap0):facts0,
	 ((Map)pMap1):facts1,
	 ((Map)pMap2):facts2,
	 ((Map)pMap3):facts3,
	 ((Map)pMap4):facts4,
	 ((Map)pMap5):facts5,
	 ((Map)pMap6):facts6,
	 ((Map)pMap7):facts7,
	 ((Map)pMap8):facts8,
	 ((Map)pMap9):facts9,
	 ((Map)pMap10):facts10,
	 ((Map)pMap11):facts11,
	 ((Map)pMap12):facts12,
	 ((Map)pMap13):facts13,
	 ((Map)pMap14):facts14,
	 ((Map)pMap15):facts15,
	 ((Map)pMap16):facts16,
	 ((Map)pMap17):facts17,
	 ((Map)pMap18):facts18,
	 ((Map)pMap19):facts19,
	 ((Map)pMap20):facts20,
	 ((Map)pMap21):facts21,
	 ((Map)pMap22):facts22,
	 ((Map)pMap23):facts23,
	 ((Map)pMap24):facts24,
	 ((Map)pMap25):facts25,
	 ((Map)pMap26):facts26,
	 ((Map)pMap27):facts27,
	 ((Map)pMap28):facts28,
	 ((Map)pMap29):facts29,
	 ((Map)pMap30):facts30,
	 ((Map)pMap31):facts31,
	 ((Map)pMap32):facts32,
	 ((Map)pMap33):facts33,
	 ((Map)pMap34):facts34,
	 ((Map)pMap35):facts35,
	 ((Map)pMap36):facts36,
	 ((Map)pMap37):facts37,
	 ((Map)pMap38):facts38,
	 ((Map)pMap39):facts39,
	 ((Map)pMap40):facts40,
	 ((Map)pMap41):facts41,
	 ((Map)pMap42):facts42,
	 ((Map)pMap43):facts43,
	 ((Map)pMap44):facts44,
	 ((Map)pMap45):facts45,
	 ((Map)pMap46):facts46,
	 ((Map)pMap47):facts47];

for( Map factMap : pMaps.keySet()){
   for (Predicate p : factMap.keySet() ){
        insert = data.getInserter(p,pMaps[factMap]);
    	java.io.File f = new File(factMap[p]);
	if(f.exists()){
	    System.out.println("Loading files "+factMap[p]);
	    InserterUtils.loadDelimitedDataTruth(insert,factMap[p]);
	} else {
	    System.out.println("Error!  No file "+factMap[p]);
	}
   }
}


Partition training = new Partition(22);

insert = data.getInserter(Cat, training);
System.out.println("Loading files " + dataroot + "label-train-uniq-raw-cat.db.TRAIN");
InserterUtils.loadDelimitedDataTruth(insert, dataroot + "label-train-uniq-raw-cat.db");

insert = data.getInserter(Rel, training);
System.out.println("Loading files " + dataroot + "label-train-uniq-raw-rel.db.TRAIN");
InserterUtils.loadDelimitedDataTruth(insert, dataroot + "label-train-uniq-raw-rel.db");


System.out.println("[info] data loading finished")

Date stop = new Date();

TimeDuration td = TimeCategory.minus( stop, start );
System.out.println td;
