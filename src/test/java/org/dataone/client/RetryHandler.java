/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */

package org.dataone.client;

import org.dataone.service.exceptions.BaseException;

public abstract class RetryHandler<V> {

    public static class TryAgainException extends Exception {
        private static final long serialVersionUID = 7048228208792138012L;
    }

    
    /**
     * Executes the attempt() method at regular intervals until no exception
     * is raised, or the duration has been reached.
     * @param interval - milliseconds between attempts
     * @param duration - the maximum number of milliseconds to try getting
     *                   the desired result.
     * @return
     * @throws Exception
     */
    public V execute(long interval, long duration) throws Exception {

        long timeRemaining = duration;
        while (timeRemaining >= 0) {
            try {
                return attempt();
            } catch (TryAgainException e) {
                
                Thread.sleep(interval);
                timeRemaining -= interval;
                if (timeRemaining < 0) {
                	// try to not throw a TryAgainException outside of the RetryHandler
                	// throw the encapsulated one instead
                    if(e.getCause() instanceof BaseException)
                        throw (BaseException) e.getCause();
                    throw e;
                }
            }
        }
        return null;
    }
    
    /**
     * the task to attempt
     * @return 
     * @throws TryAgainException - should be thrown by implementations for non-successful
     *                             attempts where a retry is warranted
     * @throws Exception - all other exceptions.  Will immediately exit the retry logic
     */
    protected abstract V attempt() throws TryAgainException, Exception;

}
