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

import java.io.*;
import java.util.*;
import java.util.HashSet;

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
  resultsDB.close()
}


def createModel(data){
  Logger log = LoggerFactory.getLogger(this.class);
  PSLModel m = new PSLModel(this, data);

  seedWt = 7500;
  trainWt = 5000;
  constrWt = 100;
  erWt = 10;
  candWt = 1;
  pattWt = 1;
  promWt = 1;
  priorWtPos = 1;
  priorWtNeg = 0.01;

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

  sqPotentials = true;
  sqOntoPotentials = true;//false;

  ///////////////////////////// rules ////////////////////////////////////
  log.info("[info] \t\tREADING RULES...");

  m.add rule:  ( SameEntity(A,B) & Cat(A,C) )  >> Cat(B,C) ,
		      squared: sqPotentials,
		      weight : weightMap["ERCat"];
  //    weight: 24.3174//erWt;

  m.add rule: ( SameEntity(A,B) & Rel(A,Z,R) )  >> Rel(B,Z,R) ,
		      squared: sqPotentials,
		      weight : weightMap["ERRelSubj"];
  //    weight: 24.1674;//erWt;

  m.add rule: ( SameEntity(A,B) & Rel(Z,A,R) ) >> Rel(Z,B,R) ,
		      squared: sqPotentials,
		      weight : weightMap["ERRelObj"];
  //    weight: 24.268752804084627//erWt;

  /**/

  m.add rule: ( Sub(C,D) & Cat(A,C) ) >> Cat(A,D) ,        
  //                   constraint : true ;
		      squared: sqOntoPotentials,
		      weight : weightMap["Sub"];
  //    weight: 100 //constrWt;

  m.add rule: ( RSub(R,S) & Rel(A,B,R) ) >> Rel(A,B,S), 
  //                   constraint : true ;
		      squared: sqOntoPotentials,
		      weight : weightMap["RSub"];
  //    weight: 99.95404470039401// constrWt;


  m.add rule: ( Mut(C,D) & Cat(A,C) ) >> ~Cat(A,D),
  //                   constraint : true ;
		      squared: sqOntoPotentials,
		      weight : weightMap["Mut"];
  //    weight: 23.176120737816692///constrWt;


  m.add rule: ( RMut(R,S) & Rel(A,B,R) ) >> ~Rel(A,B,S),
  //                   constraint : true ;
		      squared: sqOntoPotentials,
		      weight : weightMap["RMut"];	  
  //    weight: 100///constrWt;


  m.add rule: ( Inv(R,S) & Rel(A,B,R) ) >> Rel(B,A,S),
  //                   constraint : true ;
		      squared: sqOntoPotentials,
		      weight : weightMap["Inv"];	  
  //    weight: 99.94383801470507/// constrWt;


  m.add rule: ( Domain(R,C) & Rel(A,B,R) ) >> Cat(A,C),
  //                   constraint : true ;
		      squared: sqOntoPotentials,
		      weight : weightMap["Domain"];
  //    weight: 99.93756554876204///constrWt;


  m.add rule: ( Range2(R,C) & Rel(A,B,R) ) >> Cat(B,C),
  //                   constraint : true ;
		      squared: sqOntoPotentials,
		      weight : weightMap["Range"];
  //    weight: 99.91164648719196 /// constrWt;


  m.add rule: ( CandCat(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : candWt;
  m.add rule: ( CandRel(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : candWt;

  m.add rule: ( CandCat_General(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : weightMap["CandCat_General"];
  //    weight : 1.9247474887642064///candWt;
  m.add rule: ( CandRel_General(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["CandRel_General"];
  //    weight : 0.05093475899009189///candWt;

  m.add rule: (CandCat_CBL(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : weightMap["CandCat_CBL"];
  //    weight : 0.06961061036384489///candWt;
  m.add rule: (CandRel_CBL(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["CandRel_CBL"];
  //    weight : 0.026966648574574422///candWt;
  m.add rule: (CandCat_CMC(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : weightMap["CandCat_CMC"];
  //    weight : 0.006874172024394283///candWt;
  m.add rule: (CandRel_CMC(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["CandRel_CMC"];
  //    weight : candWt;
  m.add rule: (CandCat_CPL(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : weightMap["CandCat_CPL"];
  //    weight : 0.012402610094926787///candWt;
  m.add rule: (CandRel_CPL(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["CandRel_CPL"];
  //    weight : 0.024562654639784073///candWt;
  m.add rule: (CandCat_Morph(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : weightMap["CandCat_Morph"];
  //    weight : 0.03414500051864742///candWt;
  m.add rule: (CandRel_Morph(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["CandRel_Morph"];
  //    weight : candWt;
  m.add rule: (CandCat_SEAL(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : weightMap["CandCat_SEAL"];
  //    weight : 0.018952544686577127///candWt;
  m.add rule: (CandRel_SEAL(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["CandRel_SEAL"];
  //    weight : 0.08174686570602371///candWt;
  m.add rule: (PattCat(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : weightMap["PattCat"];
  //    weight : 0.12767258704250636///pattWt;
  m.add rule: (PattRel(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["PattCat"];
  //    weight : 0.15589337293916514///pattWt;


  m.add rule: (PromCat(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : promWt;
  m.add rule: (PromRel(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : promWt;
  m.add rule: (PromCat_General(A,C) ) >> Cat(A,C),
		      squared: sqPotentials,
		      weight : weightMap["PromCat_General"];
  //    weight : 0.03488693263677389//promWt;
  m.add rule: (PromRel_General(A,B,R) ) >> Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["PromRel_General"];
  //    weight : 0.04541723828669436//promWt;

  m.add rule: ~Cat(A,C),
		      squared: sqPotentials,
		      weight: weightMap["negPriorCat"];
  //weight: 0.03529481589587765///priorWtNeg;

  m.add rule: ~Rel(A,B,R),
		      squared: sqPotentials,
		      weight : weightMap["negPriorRel"];
  //    weight: 0.012581362993398976///priorWtNeg;

  log.info(m.toString());
  return m;
}

def runInference(m, data, factPartition){
  Logger log = LoggerFactory.getLogger(this.class);
  Partition ontology = new Partition(10);
  Partition seeds = new Partition(40);
  Partition training = new Partition(22);
  Partition writeInfAll = new Partition(55);
  data.deletePartition(writeInfAll);
  log.info("Starting Inference on Partition "+factPartition.getID());
  ConfigManager cm = ConfigManager.getManager();
  ConfigBundle inferenceBundle = cm.getBundle("inference");
  inferenceBundle.addProperty("admmreasoner.maxiterations",5000);
  inferenceBundle.addProperty("lazympeinference.maxrounds",14);

  HashSet closedPredsAll = new HashSet<StandardPredicate>([Name,Sub,RSub,Mut,RMut,Inv,Domain,Range2,SameEntity,CandCat,CandRel,CandCat_General,CandRel_General,CandCat_CBL,CandCat_CMC,CandCat_CPL,CandCat_Morph,CandCat_SEAL,CandRel_CBL,CandRel_CPL,CandRel_SEAL,PromCat_General,PromRel_General,SeedCat,SeedRel,TrCat,TrRel,ValCat,ValRel]);

  Database inferenceDB = data.getDatabase(writeInfAll, closedPredsAll, factPartition, ontology, training, seeds);
  mpe = new LazyMPEInference(m, inferenceDB, inferenceBundle);
  result = mpe.mpeInference();
  inferenceDB.close();

  log.info("STATUS: Inference complete - Partition "+factPartition.getID());
  print_results(data, writeInfAll);

}

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

DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, './psl', false), new EmptyBundle());
PSLModel m = createModel(data);
facts = [facts5, facts4, facts3, facts2, facts1, facts0, facts012345, facts012, facts345, facts01, facts23, facts45];
for ( Partition p : facts ){
  runInference(m, data, p);
}
