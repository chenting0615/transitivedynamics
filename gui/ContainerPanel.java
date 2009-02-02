package gui;

import io.ModelIO;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.util.ArrayList;

import util.Util;


import javax.swing.JPanel;


public class ContainerPanel extends JPanel {
	
	private static final long serialVersionUID = 9181592230994808685L;
		
	protected boolean continueExplore = true;
	
	protected static boolean showConstraints = false;
	
	protected GridBagConstraints gbc;
		
	/**
	 * Display Magnitudes 
	 */
	private static boolean isMagnitude = false;
	
	/**
	 * Display TCR
	 */
	private static boolean isTCR = false;
	
	/**
	 * Holds the interactive panels for the user to play with.
	 */
	public InteractivePanel panels[];
	
	/**
	 * Allows users to constrain our panels
	 */
	public ConstrainPanel constrain[];
	
	/**
	 * The conclusion panel used to show the final conclusion of the model
	 */
	public ConclusionPanel conclusion;
	
	/**
	 * lastGood is our array holding the last known working configuration.
	 * [panel#][0..4] -> iA,iB,iR,iE
	 * Initialized when container panel is created
	 */
	protected int[][] lastGood;
	
	
//---------------------------------------------------------------------------------------
// 	PUBLIC METHODS
	
	
	/**
	 * Default constructor for the container panel.  Creates two premises to start.
	 */
	public ContainerPanel() {
		super();
				
		setLayout(new GridLayout(0, 1));
				
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
        setBackground(Color.white);
		
        createModel(2);
		
        setVisible(true);
	}
	
	
	/**
	 * Creates a new model with the given number of premises.  The number of premises must
	 * be between 2 and 5.
	 * 
	 * @param num the number of premises
	 */
	public void createModel(int num) {
		if (num < 2 || num > 25) return;
		
		isMagnitude = false;
		isTCR = false;
		
		// Remove the current model
		removeAll();
		
		// Reset the arrays
		panels = new InteractivePanel[num];
		lastGood = new int[num][4];
		constrain = new ConstrainPanel[num-1];
									
		// Create the first premise
		panels[0] = new InteractivePanel(this, 0, "A", "B");
		panels[0].myConstrainBelow = constrain[0];
		
		// Create the rest of the premises
		char curLetter = 'B';
		for (int i = 1; i < num; i++) {
			panels[i] = new InteractivePanel(this, i, String.valueOf(curLetter++), String.valueOf(curLetter));
			constrain[i-1] = new ConstrainPanel(this, panels[i-1], panels[i]);
			panels[i].myConstrainAbove = constrain[i-1];
			if (i != num-1) panels[i].myConstrainBelow = constrain[i];
		}
				
		conclusion = new ConclusionPanel(this);
				
		// Setup the panels
		for (int i = 0; i < panels.length - 1; i++) {
			panels[i].setChild(panels[i + 1]);
			panels[i + 1].setParent(panels[i]);
			
			add(panels[i],gbc);
			if (showConstraints)
				add(constrain[i]);
		}
		add(panels[panels.length - 1]);
		add(conclusion);
		conclusion.setParent(panels[panels.length - 1]);
		
		update();
		
		// Set all the panels as causes
		for (int i = 0; i < panels.length; i++) {
			setPanel(i, Util.CAUSES);
		}
				
		updateUI();
	}
	
	/**
	 * Removes and re-adds all panels in the model
	 */
	public void reDrawModel()
	{
		removeAll();
		
		for (int i = 0; i < panels.length - 1; i++)
		{
			add(panels[i],gbc);
			if (showConstraints)
				add(constrain[i]);
		}
		add(panels[panels.length - 1]);
		add(conclusion);
		
		updateUI();
	}

	/**
	 * Returns a string representation of the current container panel.
	 * 
	 * @return the string representation of this container panel
	 */
	public String toString() {
		String toReturn = panels.length + ModelIO.SEPARATOR;
		
		// The panel information
		for (int i = 0; i < panels.length; i++) {
			toReturn += panels[i].toString();
			if (i < panels.length - 1) toReturn += ModelIO.SEPARATOR;
		}
		
		for (int i = 0; i<constrain.length; i++) {
			toReturn += constrain[i].toString() + ModelIO.SEPARATOR;
		}
		
		return toReturn;
	}
	
	/**
	 * Updates the output for all panels.
	 */
	public void update() {
		for(int i=0; i<panels.length; i++) {
			InteractivePanel tmp = panels[i];
		
			tmp.setA(getBestA(tmp));
			tmp.setB(getBestB(tmp));
			lockNeededVecs(tmp);
			setBestSubscript(tmp);
			tmp.updateValues();
			
			if(i>0) constrain[i-1].update();
		}
		
		boolean changed = differsFromRestore();
		boolean updateConclusion = false;
		
		//Verify all vectors are consistent
		if(consistent()) 
		{ 
			updateLast();
			if(changed) 
			{
				updateConclusion = true;
				for(int i=0; i<panels.length; i++) 
					panels[i].repaint();
			}
		} 
		else
			restoreLast();
		
		for(int i=0; i<constrain.length; i++) {
			constrain[i].updateLabel();
		}
		
		if(conclusion != null && updateConclusion) { 
			conclusion.update();
			conclusion.repaint();
		}
	}
	
	/**
	 * Repaints all Panels in the model
	 */
	public void repaintAll() {
		for(int i=0; i<panels.length; i++) 
			panels[i].repaint();
		
		if (showConstraints)
			for (int i = 0; i < panels.length - 1; i++)
				constrain[i].repaint();
		
		conclusion.repaint();
	}
	
	
//---------------------------------------------------------------------------------------
// 	ACCESSORS AND MUTATORS

	public void setTCR(boolean tcr) {
		isTCR = tcr;
	}
	
	public void setMagnitudes(boolean mag) {
		isMagnitude = mag;
	}
	
	public boolean isMagnitude () 
	{
		return isMagnitude;
	}
	
	public boolean isTCR ()
	{
		return isTCR;
	}
	
	public void setShowConstrain(boolean constrain) {
		showConstraints = constrain;
		reDrawModel();
	}
	
	public void setPanel (int panelNum, String desiredReln) {
		String[] verbs = new String[panels.length];
		for (int i = 0; i < panels.length; i++) {
			if (i == panelNum)
				verbs[i] = desiredReln;
			else
				verbs[i] = panels[i].verb;
		}
		setPanels(verbs);
	}
	
	/**
	 * Sets our model to the given verbs. This is what makes the model 
	 * click-changable. 
	 * 
	 * 1. Set correct verbs
	 * 2. Lock needed vectors
	 * 3. Create hierarchy of variable vectors
	 * 4. Vary vectors and check if the model is consistent
	 * 5. Expand the correct configuration outwards
	 */
	public void setPanels (String[] verbs) {
		if (verbs.length != panels.length)
			return;
		
		int initE = panels[0].iE;
		
		updateLast();
		
		// Setup the correct verbs
		for (int i = 0; i < panels.length; i++)
			panels[i].verb = verbs[i];
		
		// Lock the needed vectors
		for (int i = 0; i < panels.length; i++)
			lockNeededVecs(panels[i]);
		
		// Create Vector hierarchy
		int max_var = panels.length+1;
		ArrayList<Vector> vecs = new ArrayList<Vector>();
		for (InteractivePanel p : panels) {
			vecs.add(new Vector(p,Panel.eVEC,max_var));
			if (!p.rALocked)
				vecs.add(new Vector(p,Panel.aVEC,max_var));
			if (!p.rBLocked)
				vecs.add(new Vector(p,Panel.bVEC,max_var));
		}
		
		// Vary and check consistency
		int vec = vecs.size()-1;
		while (!consistent(verbs) && vec >= 0) {
			boolean upgrade_necessary = vecs.get(vec).vary();
			if (upgrade_necessary)
				vec -= 1;
			else
				vec = vecs.size()-1;
		}
		
		// Expand our configuration outwards
		double multiplier = initE / (double) panels[0].iE;
		for (Panel p : panels) {
			p.iE = (int) Math.round(multiplier * p.iE);
			p.iA = (int) Math.round(multiplier * p.iA);
			p.iB = (int) Math.round(multiplier * p.iB);
		}
	}
	
	/**
	 * Updates the model and checks to make sure that 
	 * each panel indeed has the desired verb. If so,
	 * return true. Else false;
	 */
	boolean consistent (String[] verbs) {
		updateInsideOut();
		for (int i = 0; i < panels.length; i++) {
			panels[i].updateValues();
			if (!panels[i].verb.equals(verbs[i]))
				return false;
		}
		return true;
	}
	
	/**
	 * The vector class is a wrapper for a single vector element 
	 * such as iA,iB, or iE. It has a reference to its parent 
	 * vector and a string to identify which field to manipulate.
	 * It also know something of how to vary the vectors. 
	 *
	 */
	class Vector { 
		/**
		 * The maximum variance allowed. At a mininum this should be equal
		 * to the number of panels in the model.
		 */
		int MAX_VAR = 3;
		
		Panel p;
		String field;
		int curr_mag;
		
		public Vector (Panel panel, String _field, int max_var) {
			MAX_VAR = max_var;
			p = panel;
			field = _field;
			curr_mag = MAX_VAR;
			updatePanel();
		}
		
		/**
		 * Varies the vector within the maximum variance. 
		 * returns true when we need to upgrade and false
		 * otherwise.
		 */
		public boolean vary () {
			if (field.equals(Panel.eVEC))
				return varyE();
			curr_mag -= 1;
			if (curr_mag == 0) //Excluding zero makes the expansion easier
				curr_mag -= 1;
			updatePanel();
			if (curr_mag == MAX_VAR)
				return true;
			if (curr_mag == -MAX_VAR)
				curr_mag = MAX_VAR+1;	
			return false;
		}
		
		/**
		 * A special method to vary our E vector:
		 * It only needs to be varied at MAX_VAR 
		 * and -MAX_VAR
		 */
		boolean varyE () {
			curr_mag *= -1;
			updatePanel();
			if (curr_mag == MAX_VAR) {
				return true;
			} else {
				return false;
			}
		}
		
		/**
		 * Pushes our current magnitude onto the actual panel
		 */
		void updatePanel () {
			if (field.equals(Panel.aVEC))
				p.iA = curr_mag;
			if (field.equals(Panel.bVEC))
				p.iB = curr_mag;
			if (field.equals(Panel.eVEC))
				p.iE = curr_mag;
		}
	}
	
	/**
	 * Sets a panel to a specific verb
	 */
	public boolean setPanel(int num, int value) {
		int eWidth = (panels.length * 50) + 50;
		if (eWidth > 500) eWidth = 500;
		
		String verb = "Causes";
		if (value == Util.HELPS) verb = "Helps";
		if (value == Util.PREVENTS) verb = "Prevents";
		if (value == Util.DESPITE) verb = "Despite";
		
		// We have specific, easy setups for the first premise
		if (num == 0) {
			switch (value) {
			case Util.CAUSES:
				panels[0].iE = eWidth;
				panels[0].iA = (eWidth * 2) / 3;
				panels[0].iB = -eWidth / 3;
				break;
				
			case Util.HELPS:
				panels[0].iE = eWidth;
				panels[0].iA = eWidth / 4;
				panels[0].iB = eWidth / 2;
				break;
				
			case Util.PREVENTS:
				panels[0].iB = eWidth / 3;
				
				panels[0].iA = -(eWidth * 2) / 3;
				break;
				
			case Util.DESPITE:
				panels[0].iE = -eWidth;
				panels[0].iA = eWidth / 3;
				panels[0].iB = (-eWidth * 2) / 3;
				break;
		}
			
			if (panels[0].aNegated) panels[0].iA *= -1;
			if (panels[0].eNegated) panels[0].iE *= -1;
			
		// Anything else, we have to base it off of the existing value (if possible)
		} else {
			int startA = panels[num].iA;
			int startB = panels[num].iB;
			int startE = panels[num].iE;
			
			int dir = (panels[num].iA >= 0) ? 1 : -1;
			if (panels[num].aNegated) dir = -dir;
			
			// Setup the e value
			if (panels[num].eNegated) {
				if (value == Util.CAUSES || value == Util.HELPS)
					panels[num].iE = -dir * eWidth;
				else
					panels[num].iE = dir * eWidth;
				
			} else {
				if (value == Util.CAUSES || value == Util.HELPS)
					panels[num].iE = dir * eWidth;
				else
					panels[num].iE = -dir * eWidth;
			}
			
			
			int low = -eWidth;
			int high = eWidth;
			
			// Move B to find the lower premise
			panels[num].iB = -eWidth;
			update();
			while (!panels[num].verb.equals(verb) && panels[num].iB <= eWidth) {
				panels[num].iB++;
				panels[num].updateValues();
			}
			
			if (panels[num].verb.equals(verb)) low = panels[num].iB;
			else {
				panels[num].iE = startE;
				panels[num].iA = startA;
				panels[num].iB = startB;
				panels[num].updateValues();
				panels[num].repaint();
				
				return false;
			}
			
			while (panels[num].verb.equals(verb) && panels[num].iB <= eWidth) {
				panels[num].iB++;
				panels[num].updateValues();
			}
			
			high = panels[num].iB;
			
			panels[num].setB((low + high) / 2);
		}
		
		update();
		panels[num].repaint();
		
		return true;
	}
	
	/**
	 * Returns an array of strings containing the names
	 * of the affector and patient vectors for all panels
	 * @return
	 */
	public String[] getVecNames ()
	{
		int numVecs = panels.length * 2 + 2;
		String[] vecNames = new String[numVecs + 1];
		int vecIndex = 0;
		vecNames[vecIndex++] = "";
		
		for (int i = 0; i < panels.length; i++)
		{
			String affVec = panels[i].getAWord();
			if (panels[i].getASubscript() != null)
				affVec += " (" + panels[i].getASubscript() + ")";
			String patVec = panels[i].getBWord();
			if (panels[i].getBSubscript() != null)
				patVec += " (" + panels[i].getBSubscript() + ")";
			vecNames[vecIndex++] = affVec;
			vecNames[vecIndex++] = patVec;
		}
		
		String conclusionA = "conclusion " + conclusion.getAWord();
		if (conclusion.getASubscript() != null)
			conclusionA += " (" + conclusion.getASubscript() + ")";
		String conclusionB = "conclusion " + conclusion.getBWord();
		if (conclusion.getBSubscript() != null)
			conclusionB += " (" + conclusion.getBSubscript() + ")";
		
		vecNames[vecIndex++] = conclusionA;
		vecNames[vecIndex++] = conclusionB;
		
		return vecNames;
	}
	
//---------------------------------------------------------------------------------------
// 	PRIVATE METHODS
	
	/**
	 * Returns the best A vector.
	 */
	private int getBestA(InteractivePanel i) {
		if(i.parent != null && i.parent.constrainedAbove) {
			return i.parent.iA + i.parent.iB + i.parent.parent.iA;
		}
		if(isUpAligned(i)) {
			if(i.parent == null || isUpAligned(i.parent)) return i.getA();
			else return (getBestA(i.parent) + getBestB(i.parent));
		} else { //Non-prevent verb : Help etc
			if(i.parent == null || isUpAligned(i.parent)) return i.getA();
			else return (getBestA(i.parent) + getBestB(i.parent));
		}
	}
	
	/**
	 * Returns the best B vector.
	 */
	private int getBestB(InteractivePanel i) {
		if(isUpAligned(i)) {
			if(i.child == null || i.constrainedAbove) return i.getB();
			else return (getBestA(i.child) + getBestB(i.child));
		} else { //Non-prevent verb : Cause etc
			return i.getB();
		}
	}
	
	/**
	 * Sets lock-down on vectors which should be untouchable.
	 * Uses a set of rules to determine if A or B should be locked.
	 */
	private void lockNeededVecs(InteractivePanel i) {	
		if(isUpAligned(i)) {
			if(i.parent == null) { //Prevent || Cause_N with null parent
				i.rALocked = false; i.rBLocked = true;
			} else if(i.child == null) { //Prevent || Cause_N with null child
				if(!isUpAligned(i.parent) || i.parent.constrainedAbove) {//Prevent as last panel with non-prevent parent
					i.rALocked = true; i.rBLocked = false;
				} else {//Prevent as last panel with prevent parent
					i.rALocked = i.rBLocked = false;
				}
			} else if(!isUpAligned(i.parent)) { //Prevent with parent non-prevent
				if(!isUpAligned(i.child)) { //Prevent between two non-prevents
					if (i.constrainedAbove) {
						i.rALocked = true; i.rBLocked = false;
					} else {
						i.rALocked = i.rBLocked = true;
					}
				} else { //Prevent with non-prevent parent and prevent child
					i.rALocked = true; i.rBLocked = true;
				}
			} else { //PPP
				if (!i.constrainedAbove) {
					if (i.parent.constrainedAbove) {
						i.rALocked = true; i.rBLocked = true;
					} else {
						i.rALocked = false; i.rBLocked = true;
					}
				} else {
					i.rALocked = false; i.rBLocked = false;
				}
			}
		} else { //Non-prevent verb
			if(i.parent == null) { //Cause with null parent always free
				i.rALocked = i.rBLocked = false;
			} else if(i.child == null && isUpAligned(i.parent)) {
				if (i.parent.constrainedAbove) {
					i.rALocked = true; i.rBLocked = false;
				} else {
				i.rALocked = i.rBLocked = false; 
				}
			} else if(isUpAligned(i.parent)) {
				if (i.parent.constrainedAbove) {
					i.rALocked = true; i.rBLocked = false;
				} else {
					i.rALocked = i.rBLocked = false;
				}
			} else {
				i.rALocked = true; i.rBLocked = false;
			}
		}
	}
	
	/**
	 * Updates the locked vectors in a given panel. This assumes
	 * that the correct locked vectors have already been established
	 * and that the parent and child's iR vectors have already been 
	 * put in the correct places.
	 */
	void updateLocked (InteractivePanel i) {
		if (i.rALocked)
			i.iA = i.parent.iR;
		if (i.rBLocked)
			i.iB = i.child.iR;
	}
	
	/**
	 * Updates safely: starts updating only at vectors
	 * with no locks. Slowly works its way out toward vectors 
	 * which have locks.
	 * 
	 * Vectors must be correctly locked before this is called!
	 */
	void updateInsideOut () {
		ArrayList<InteractivePanel> safe_list = new ArrayList<InteractivePanel>();
		ArrayList<InteractivePanel> unsafe_list = new ArrayList<InteractivePanel>();
		
		for (InteractivePanel p : panels)
			unsafe_list.add(p);
		
		while (unsafe_list.size() > 0) {
			for (int i = 0; i < unsafe_list.size(); i++) {
				InteractivePanel p = unsafe_list.get(i);
				if (!p.rALocked && !p.rBLocked) { //Panel is inherently update safe
					updateTransfer(p,safe_list,unsafe_list);
				} else if (p.rALocked && safe_list.contains(p.parent)) {
					updateTransfer(p,safe_list,unsafe_list);
				} else if (p.rBLocked && safe_list.contains(p.child)) {
					updateTransfer(p,safe_list,unsafe_list);
				}
			}
		}
	}
	
	/**
	 * Updates and transfer a panel from the unsafe list to the safe list.
	 * Used only in the context of the above method.
	 */
	void updateTransfer (InteractivePanel p, ArrayList<InteractivePanel> safe,
			ArrayList<InteractivePanel> unsafe) {
		updateLocked(p);
		p.updateR();
		safe.add(p);
		unsafe.remove(p);
	}
	
	/**
	 * Determines if inheritance of result vector should take place upwards
	 * @return true if upWards
	 */
	private boolean isUpAligned(InteractivePanel i) {
		if(i.verb.equals("Prevents") || (i.verb.equals("Causes") && i.eNegated))
			return true;
		return false;
	}
	
	/**
	 * Finds an appropriate subscript for any locked vectors in panel i.
	 * Every Locked vector should have a subscript.
	 */
	private void setBestSubscript(InteractivePanel i) {
		if(i.parent == null && i.child == null) return;
		if(!i.getALocked() && !i.getBLocked()) { //Neither vector locked so none should have subscript
			i.setASubscript(null); i.setBSubscript(null); 
		}
		if(i.getALocked()) { //a vector locked - take subscript from parent
			i.setASubscript(i.parent.rWord);
			if(!i.getBLocked() && i.getBSubscript() != null) i.setBSubscript("");
		}
		if(i.getBLocked()) { //b vector locked - take subscript from child
			i.setBSubscript(i.child.rWord);
			if(!i.getALocked() && i.getASubscript() != null) i.setASubscript("");
		}	
	}
	
	/**
	 * Updates the last valid restore point. This should only
	 * happen if the current model is consistent.
	 */
	private void updateLast() {
		for(int i=0; i<panels.length; i++) {
			lastGood[i][0] = panels[i].iA;
			lastGood[i][1] = panels[i].iB;
			lastGood[i][2] = panels[i].iR;
			lastGood[i][3] = panels[i].iE;
		}
	}
	
	/**
	 * Restore the model to the last valid point
	 */
	private void restoreLast() {
		for(int i=0; i<panels.length; i++) {
			panels[i].iA = lastGood[i][0];
			panels[i].iB = lastGood[i][1];
			panels[i].iR = lastGood[i][2];
			panels[i].iE = lastGood[i][3];
			panels[i].updateValues();
			lockNeededVecs(panels[i]);
			setBestSubscript(panels[i]);
		}
	}
	
	/**
	 * Detect if the current model differs from the last restore point.
	 * 
	 * @return true if differing
	 */
	private boolean differsFromRestore() {
		for(int i=0; i<panels.length; i++) {
			if(panels[i].iA != lastGood[i][0]) return true;
			if(panels[i].iB != lastGood[i][1]) return true;
			if(panels[i].iR != lastGood[i][2]) return true;
			if(panels[i].iE != lastGood[i][3]) return true;
		}
		return false;
	}
	
	/**
	 * This method checks for any vectors whose math does not add up 
	 * @return true if we are all consistent, false otherwise
	 */
	private boolean consistent() {
		for(int i=0; i<panels.length; i++) 
		{
			if(panels[i].rALocked) {
				if ((panels[i-1].iR == panels[i].iA) || 
					(panels[i-1].constrainedAbove && 
					panels[i].iA == panels[i-1].iB + panels[i-1].iA + panels[i-2].iA)) {
					// We're good. Do nothing.
				}else {
					return false;
				}
			}
			if(panels[i].rBLocked && panels[i+1].iR != panels[i].iB)
				return false;
		}
		return true;
	}
}
