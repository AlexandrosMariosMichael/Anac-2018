package AgentExp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Domain;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.ValueDiscrete;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;
import negotiator.utility.UtilitySpace;

public class AgentExp extends AbstractNegotiationParty {

	// start-up values - init

	private Bid lastReceivedBid = null;
	private Domain domain = null;

	private List<BidInfo> lBids;

	private double threshold_low = 0.9999;
	private double threshold_high = 1.0;

	@Override
	public void init(NegotiationInfo info) {

		super.init(info);
		this.domain = info.getUtilitySpace().getDomain();

		// create list of bids & sort it descending
		lBids = new ArrayList<>(AgentTool.generateRandomBids(this.domain, 30000, this.rand, this.utilitySpace));
		Collections.sort(lBids, new BidInfoComp().reversed());

	}

	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) {

		// Setting compromise degree

		threshold_high = 1 - 0.1 * timeline.getTime();
		threshold_low = 1 - 0.1 * timeline.getTime() - 0.0001 * Math.exp(this.timeline.getTime());
		
		System.out.println("max util: "+threshold_high+"\nmin util: "+threshold_low);
		
		// if time is running out
		// drops standards to reach an agreement
		if (timeline.getTime() > 0.99) {						
			threshold_low = 1 - 0.2718 * timeline.getTime();		
		}

		/** 
	 	* Function timeline.getTime()
        * Gets the time, running from t = 0 (start) to t = 1 (deadline). The time
        * is normalized, so agents need not be concerned with the actual internal
        * clock.
        *
        * @return current time in the interval [0, 1].
        */

	
		// Accept Agreement
		if (lastReceivedBid != null) {
			if (getUtility(lastReceivedBid) > threshold_low) {
				return new Accept(getPartyId(), lastReceivedBid);
			}
		}

		// Offer selection of bids
		Bid bid = null;
		while (bid == null) {
			bid = AgentTool.selectBidfromList(this.lBids, this.threshold_high, this.threshold_low);
			if (bid == null) {
				threshold_low -= 0.0001; // every time I don't find a bid, I drop the low threshold (min utility)
			}
		}
		return new Offer(getPartyId(), bid);
	}

	@Override
	public void receiveMessage(AgentID sender, Action action) {
		super.receiveMessage(sender, action);
		if (action instanceof Offer) {
			lastReceivedBid = ((Offer) action).getBid();
		}
	}

	@Override
	public String getDescription() {
		return "AgentExp - tucANAC2018-19";
	}

}


class AgentTool {

	private static Random random = new Random();

	public static Bid selectBidfromList(List<BidInfo> bidInfoList, double higherutil, double lowerutil) {
		List<BidInfo> bidPool = new ArrayList<>();
		for (BidInfo bidInfo : bidInfoList) {				// select random bids, but only if they meet util requirements add them to bid pool
			if (bidInfo.getutil() <= higherutil && bidInfo.getutil() >= lowerutil) {
				bidPool.add(bidInfo);
			}
		}
		
		if (bidPool.size() == 0) {		// no bids within min - max thresholds 
			return null;
		} else {
			return bidPool.get(random.nextInt(bidPool.size())).getBid();  //pick random bid from 2nd list
		}
	}

	// one-time (init) creation of main bid list 
	public static Set<BidInfo> generateRandomBids(Domain d, int numberOfBids, Random random, UtilitySpace utilitySpace) {
		Set<BidInfo> randombidsPool = new HashSet<>();
		for (int i = 0; i < numberOfBids; i++) {
			Bid b = d.getRandomBid(random);
			randombidsPool.add(new BidInfo(b, utilitySpace.getUtility(b)));
		}
		return randombidsPool;
	}

}

// Class that stores bid and its utility
class BidInfo {
	Bid bid;
	double util;

	public BidInfo(Bid b) {
		this.bid = b;
		util = 0.0;
	}

	public BidInfo(Bid b, double u) {
		this.bid = b;
		util = u;
	}

	public void setutil(double u) {
		util = u;
	}

	public Bid getBid() {
		return bid;
	}

	public double getutil() {
		return util;
	}

	// Proper implementation
	@Override
	public int hashCode() {
		return bid.hashCode();
	}

	public boolean equals(BidInfo bidInfo) {
		return bid.equals(bidInfo.getBid());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof BidInfo) {
			return ((BidInfo) obj).getBid().equals(bid);
		} else {
			return false;
		}
	}

}

final class BidInfoComp implements Comparator<BidInfo> {
	BidInfoComp() {
		super();
	}

	@Override
	public int compare(BidInfo o1, BidInfo o2) {
		return Double.compare(o1.getutil(), o2.getutil());
	}
}
