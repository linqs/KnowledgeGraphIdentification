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

class KGIUtils {
    static def loadPredicateAtoms(datastore, predicateMap, targetPartition){
	for (Predicate p : predicateMap.keySet() ){
	    
	    System.out.println("Loading files "+predicateMap[p]);
	    InserterUtils.loadDelimitedData(datastore.getInserter(p,targetPartition),predicateMap[p]);
	}
    }
    
    static def loadPredicateAtomsWithValue(datastore, predicateMap, targetPartition){
	for (Predicate p : predicateMap.keySet() ){
	    System.out.println("Loading files "+predicateMap[p]);
	    InserterUtils.loadDelimitedDataTruth(datastore.getInserter(p,targetPartition),predicateMap[p]);
	}
    }


    def initializeWeightMap() {
	
	def seedWt = 7500;
	def trainWt = 5000;
	def constrWt = 100;
	def erWt = 25;
	def candWt = 1;
	def pattWt = 1;
	def promWt = 1;
	def priorWtPos = 1;
	def priorWtNeg = 2;
	
	def weightMap = ["Sub":constrWt,
			 "RSub":constrWt,
			 "Mut":constrWt,
			 "RMut":constrWt,
			 "Inv":constrWt,
			 "Domain":constrWt,
			 "Range":constrWt,
			 "ERCat":erWt,
			 "ERRelObj":erWt,
			 "ERRelSubj":erWt,
			 "SeedCat":seedWt,
			 "SeedRel":seedWt,
			 "CandCat":candWt,
			 "CandRel":candWt,
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
			 "PromCat":promWt,
			 "PromRel":promWt,
			 "PromCat_General":promWt,
			 "PromRel_General":promWt,
			 "PattCat":pattWt,
			 "PattRel":pattWt,
			 "posPriorCat":priorWtPos,
			 "posPriorRel":priorWtPos,
			 "negPriorCat":priorWtNeg,
			 "negPriorRel":priorWtNeg];
	
	return weightMap;
    }

}

