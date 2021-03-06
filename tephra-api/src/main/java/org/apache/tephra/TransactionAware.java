/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tephra;

import java.util.Collection;

/**
 * Interface to be implemented by a component that interacts with transaction logic.
 * <p/>
 * The client code that uses transaction logic looks like this:
 * <pre>
 *  TransactionAware dataSet = // ...              // dataSet is one example of component that interacts with tx logic
 *
 *  Transaction tx = txClient.start();
 *  dataSet.startTx(tx);                           // notifying about new transaction
 *  dataSet.write(...);
 *  // ... do other operations on dataSet
 *  Collection<byte[]> changes = dataSet.getTxChanges();
 *  boolean rollback = true;
 *  if (txClient.canCommit(changes)) {             // checking conflicts before commit, if none, commit tx
 *    if (dataSet.commitTx()) {                    // try persisting changes
 *      if (txClient.commit(tx)) {                 // if OK, make tx visible; if not - tx stays invisible to others
 *        dataSet.postTxCommit();                  // notifying dataset about tx commit success via callback
 *        rollback = false;
 *      }
 *    }
 *  }
 *
 *  if (rollback) {                                // if there are conflicts (or cannot commit), try rollback changes
 *    if (dataSet.rollbackTx()) {                  // try undo changes
 *      txClient.abort(tx);                        // if OK, make tx visible; if not - tx stays invisible to others
 *    }
 *  }
 *
 * </pre>
 */
// todo: use custom exception class?
// todo: review exception handling where it is used
// todo: add flush()? nah - flush is same as commitTx() actually
// todo: add onCommitted() - so that e.g. hbase table can do *actual* deletes at this point
public interface TransactionAware {
  /**
   * Called when new transaction has started.  This may reset any state which has been left behind by the previous
   * transaction.
   *
   * @param tx transaction info
   */
  // todo: rename to onTxStart()
  void startTx(Transaction tx);

  /**
   * Called when the state of the current transaction has been updated.  This should replace any reference to the
   * current {@link Transaction} held by this {@code TransactionAware}, but should <strong>not</strong> reset
   * any state (such as the write change sets) that is currently maintained.
   *
   * @param tx the updated transaction
   */
  void updateTx(Transaction tx);

  /**
   * @return changes made by current transaction to be used for conflicts detection before commit.
   */
  Collection<byte[]> getTxChanges();

  /**
   * Called before transaction has been committed.
   * Can be used e.g. to flush changes cached in-memory to persistent store.
   * @return true if transaction can be committed, otherwise false.
   */
  // todo: rename to beforeTxCommit()
  boolean commitTx() throws Exception;

  /**
   * Called after transaction has been committed.
   * Can be used e.g. evict entries from a cache etc. Because this is called after the transaction is committed,
   * the success or failure of the transaction cannot depend on it. Hence this method returns nothing and it is not
   * expected to throw exceptions.
   * @throws RuntimeException in case of serious failure that should not be ignored.
   */
  void postTxCommit();

  /**
   * Called during transaction rollback (for whatever reason: conflicts, errors, etc.).
   * @return true if all changes made during transaction were rolled back, false otherwise (e.g. if more cleanup needed
   *         or changes cannot be undone). True also means that this transaction can be made visible to others without
   *         breaking consistency of the data: since all changes were undone there's "nothing to see".
   */
  // todo: rename to onTxRollback()
  boolean rollbackTx() throws Exception;

  /**
   * Used for error reporting.
   */
  // todo: use toString() instead everywhere
  String getTransactionAwareName();
}
