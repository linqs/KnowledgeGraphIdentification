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
    Logger log = LoggerFactory.getLogger(this.class);
    log.info("READING RULES...");

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


DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, './psl', false), new EmptyBundle());
PSLModel m = createModel(data);
facts = [facts0, facts1, facts2, facts3, facts4, facts5, facts6, facts7, facts8, facts9, facts10, facts11, facts12, facts13, facts14, facts15, facts16, facts17, facts18, facts19, facts20, facts21, facts22, facts23, facts24, facts25, facts26, facts27, facts28, facts29, facts30, facts31, facts32, facts33, facts34, facts35, facts36, facts37, facts38, facts39, facts40, facts41, facts42, facts43, facts44, facts45, facts46, facts47];

for ( Partition p : facts ){
  runInference(m, data, p);
}
