/*
 * <copyright>
 *  Copyright 2001-2003 Mobile Intelligence Corp
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.tools.robustness.ma;

import java.util.*;
import org.cougaar.util.log.*;

/**
 * Static methods for selecting a single leader from multiple candidates in
 * distributed agent network.
 */

public class LeaderElection {

  private static Logger logger = LoggerFactory.getInstance().createLogger(LeaderElection.class);

  /**
   * Returns the name of the leader if the number of votes > 0, the vote
   * is unanimous, and the vote specifies a candidate which is contained in
   * supplied set of candidates.  Otherwise the current leader is return or
   * null if current leader is no longer in candidate list.
   * @param candidates  List of candidate names
   * @param votes       Votes
   * @return Name of leader or null if no winner
   */
  public static String getLeader(Set candidates, List votes) {
    String leader = null;
    // Count the votes and see if there's a unanimous selection
    int numVotes = votes.size();
    if (numVotes == 1) {
      leader = candidates.contains(votes.get(0)) ? (String)votes.get(0) : null;
    } else if (numVotes > 1) {
      String first = (String)votes.get(0);
      for (int i = 1; i < numVotes; i++) {
        if (!first.equals((String)votes.get(i))) {
          first = null;
          break;
        }
      }
      leader = (first != null && candidates.contains(first)) ? first : null;
    }
    logger.debug("getLeader:" +
                 " candidates=" + candidates +
                 " votes=" + votes +
                 " leader=" + leader);
    return leader;
  }

  /**
   * Evaluates votes and returns the name of candidate(s) with the most votes.
   * If multiple candidates are returned due to tie they are alphabetically
   * ordered.
   * @params candidates List of candidate names
   * @param votes
   * @return Name(s) of top candidate(s)
   */
  private static SortedSet getTopCandidates(Set candidates, List votes) {
    Map voteCount = new HashMap();
    // Count votes
    for (Iterator it = votes.iterator(); it.hasNext();) {
      String candidate = (String) it.next();
      if (candidates.contains(candidate)) {
        Integer count = (Integer) voteCount.get(candidate);
        if (count == null) {
          voteCount.put(candidate, new Integer(1));
        }
        else {
          voteCount.put(candidate, new Integer(count.intValue() + 1));
        }
      }
    }
    // Find top candidates
    SortedSet leaders = new TreeSet();
    int high = 0;
    for (Iterator it = voteCount.entrySet().iterator(); it.hasNext();) {
      Map.Entry me = (Map.Entry)it.next();
      String candidate = (String)me.getKey();
      int numVotes = ((Integer)me.getValue()).intValue();
      if (numVotes > high) {
        leaders.clear();
        leaders.add(candidate);
        high = numVotes;
      } else if (numVotes == high) {
        leaders.add(candidate);
      }
    }
    return leaders;
  }

  /**
   * Determines new vote to be cast by agent.  The algorithm selects the
   * preferredCandidate if its present in the allCandidates set.  If
   * the preferredCandidate is null or not present in the allCandidates set
   * the candidate with the most votes is selected.  The following
   * list defines how some special conditions are handled:
   * <pre>
   *   -  If there is a tie for the most votes a candidate is randomly selected
   *      from the tied candidates.
   *   -  If no votes were cast in last round the a candidate is randomly
   *      selected from the allCandidates set.
   *   -  If no votes were cast and the preferred candidate is not in the
   *      allCandidates set, a candidate is randomly selected from the
   *      allCandidates set.
   *   -  If the allCandidates set is empty a null value is returned.
   * </pre>
   * @param preferredCandidate  Name of candidate to receive vote if available
   * @param allCandidates       Set of candidate names
   * @param votes               votes
   */
  public static String chooseCandidate(String preferredCandidate,
                                       SortedSet allCandidates,
                                       List votes) {
    String selected = null;
    SortedSet topCandidates = null;
    if (allCandidates.size() > 0) {
      if (allCandidates.contains(preferredCandidate)) {
        selected = preferredCandidate;
      } else {
        if (votes.size() == 0) {
          String candArray[] = (String[])allCandidates.toArray(new String[0]);
          int rand = (int)(java.lang.Math.random() * candArray.length);
          selected = candArray[rand];
        } else {
          topCandidates = getTopCandidates(allCandidates, votes);
          if (topCandidates.size() == 1) {
            selected = (String)topCandidates.first();
          } else if (topCandidates.size() > 1){
            String candArray[] = (String[])topCandidates.toArray(new String[0]);
            int rand = (int)(java.lang.Math.random() * candArray.length);
            selected = candArray[rand];
          } else {  // no top candidates, randomly select from all candidates
            String candArray[] = (String[])allCandidates.toArray(new String[0]);
            int rand = (int)(java.lang.Math.random() * candArray.length);
            selected = candArray[rand];
          }
        }
      }
    }
    logger.debug("chooseCandidate:" +
                 " preferred=" + preferredCandidate +
                 " candidates=" + allCandidates +
                 " top=" + topCandidates +
                 " votes=" + votes +
                 " selected=" + selected);
    return selected;
  }

}