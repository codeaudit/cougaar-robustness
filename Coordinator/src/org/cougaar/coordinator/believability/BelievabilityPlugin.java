/*
 *<SOURCE_HEADER>
 *
 *<NAME>
 * $RCSfile: BelievabilityPlugin.java,v $
 *</NAME>
 *
 *<RCS_KEYWORD>
 * $Source: /opt/rep/cougaar/robustness/Coordinator/src/org/cougaar/coordinator/believability/BelievabilityPlugin.java,v $
 * $Revision: 1.1 $
 * $Date: 2004-02-26 15:18:22 $
 *</RCS_KEYWORD>
 *
 *<COPYRIGHT>
 * The following source code is protected under all standard copyright
 * laws.
 *</COPYRIGHT>
 *
 *</SOURCE_HEADER>
 */

package org.cougaar.coordinator.believability;

import org.cougaar.coordinator.techspec.AssetTechSpecInterface;
import org.cougaar.coordinator.techspec.DefenseTechSpecInterface;
import org.cougaar.coordinator.techspec.ThreatModelInterface;

import org.cougaar.coordinator.timedDiagnosis.TimedDefenseDiagnosis;

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.adaptivity.ServiceUserPluginBase;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.service.EventService;
import org.cougaar.util.UnaryPredicate;


/**
 * This Plugin is used to handle the believability computation for
 * Defense Deconfliction It emits AssetBeliefState (???) objects.
 *
 */
public class BelievabilityPlugin 
    extends ServiceUserPluginBase 
    implements NotPersistable 
{
    
    //************************************************************
    /** 
      * Creates a new instance of BelievabilityPlugin 
      */
    public BelievabilityPlugin() {

        super(requiredServices);

    } // constructor BelievabilityPlugin
    

    //************************************************************
    /**
     * Called from outside. Should contain plugin initialization code.
     */
    public void load() {
        super.load();
        getPluginParams();
        haveServices();
    } // method load
    
    // Helper methods to publish objects to the Blackboard

    //************************************************************
    public boolean publishAdd(Object o) {
        getBlackboardService().publishAdd(o);
        return true;
    } // method publishAdd

    //************************************************************
    public boolean publishChange(Object o) {
     getBlackboardService().publishChange(o);
        return true;
    } // method publishChange

    //************************************************************
    public boolean publishRemove(Object o) {
     getBlackboardService().publishRemove(o);
        return true;
    } // method publishRemove
    

    //------------------------------------------------------------
    // protected interface
    //------------------------------------------------------------

    //************************************************************
    /** Called every time this component is scheduled to run. Any time
     *  objects that belong to its subscription sets change
     *  (add/modify/remove), this will be called. This method is
     *  executed within the context of a blackboard transaction (so do
     *  NOT use transaction syntax inside it).  You may only need to
     *  monitor one or two type of actions (e.g. additions), in which
     *  case you can safely remove the sections of code dealing with
     *  collection changes you are not interested in.
     */
    protected void execute() 
    {

        if (logger.isDebugEnabled()) 
            logger.debug("Believability Plugin in Execute Loop");

        handleAsset();
        handleDefense();
        handleThreatModel();
        handleDiagnosis();

    } // method execute

    //************************************************************
    /** 
      * Called from outside once after initialization, as a
      * "pre-execute()". This method sets up the subscriptions to
      * objects that we'return interested in. In this case, defense
      * tech specs and defense conditions.
      */
    protected void setupSubscriptions() {

        _assetSub 
            = ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() 
                {
                    public boolean execute(Object o) {
                        if ( o instanceof AssetTechSpecInterface) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        
        _defenseSub 
            = ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() 
                {
                    public boolean execute(Object o) {
                        if ( o instanceof DefenseTechSpecInterface) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        
        _threatModelSub 
            = ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() 
                {
                    public boolean execute(Object o) {
                        if ( o instanceof ThreatModelInterface) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        
        _diagnosisSub 
            = ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() 
                {
                    public boolean execute(Object o) {
                        if ( o instanceof TimedDefenseDiagnosis ) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        
        // Not used at this time - Will be used to provide out-of-band
        // control of this plugin
        //
        /* Commented out for now as I believe I do not need this for
         * the example I am creating.

        pluginControlSubscription = 
            ( IncrementalSubscription ) 
            getBlackboardService().subscribe
            ( new UnaryPredicate() {
                    public boolean execute(Object o) {
                        if ( o instanceof CostBenefitKnob) {
                            return true ;
                        }
                        return false ;
                    }
                }) ;
        */
        
    } // method setupSubscriptions

    //------------------------------------------------------------
    // private interface
    //------------------------------------------------------------

    //************************************************************
    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for
     * AssetTechSpecInterface objects.  
     *
     */
    private void handleAsset()
    {
        AssetTechSpecInterface asset_ts = null;

        //--------------------
        //      ADD AssetTechSpecInterface
        //--------------------

        for ( Iterator iter = _assetSub.getAddedCollection().iterator();
              iter.hasNext() ; ) 
        {
            // Add the asset to the intermediate model
            asset_ts = (AssetTechSpecInterface) iter.next(); 
            _intermediate_model.addAsset( asset_ts );

        } // for ADD AssetTechSpecInterface

        //--------------------
        //      CHANGE AssetTechSpecInterface
        //--------------------

        for ( Iterator iter = _assetSub.getChangedCollection().iterator();  
          iter.hasNext() ; ) 
        {
            asset_ts = (AssetTechSpecInterface) iter.next(); 
         _intermediate_model.changeAsset( asset_ts );
            
        } // for CHANGE AssetTechSpecInterface
        
        //--------------------
        //      REMOVE AssetTechSpecInterface
        //--------------------

        for ( Iterator iter = _assetSub.getRemovedCollection
                  ().iterator();  
          iter.hasNext() ; ) 
        {
            asset_ts = (AssetTechSpecInterface) iter.next(); 
            _intermediate_model.removeAsset( asset_ts );

        } // for REMOVE AssetTechSpecInterface

    } // method handleAsset


    //************************************************************
    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for
     * DefenseTechSpecInterface objects.  A defense is related to one or
     * more threat types.
     *
     */
    private void handleDefense()
    {
        DefenseTechSpecInterface defense_ts = null;

        //--------------------
        //      ADD DefenseTechSpecInterface
        //--------------------

        for ( Iterator iter = _defenseSub.getAddedCollection().iterator();  
              iter.hasNext() ; ) 
        {
            defense_ts = (DefenseTechSpecInterface) iter.next(); 
         _intermediate_model.addDefense( defense_ts );

        } // for ADD DefenseTechSpecInterface

        //--------------------
        //      CHANGE DefenseTechSpecInterface
        //--------------------

        for ( Iterator iter = _defenseSub.getChangedCollection
                      ().iterator();  
              iter.hasNext() ; ) 
        {

            defense_ts = (DefenseTechSpecInterface) iter.next(); 
         _intermediate_model.changeDefense( defense_ts );

        } // for CHANGE DefenseTechSpecInterface
        
        //--------------------
        //      REMOVE DefenseTechSpecInterface
        //--------------------

        for ( Iterator iter = _defenseSub.getRemovedCollection().iterator();  
              iter.hasNext() ; ) 
        {
            defense_ts = (DefenseTechSpecInterface) iter.next(); 
         _intermediate_model.removeDefense( defense_ts );

        } // for REMOVE DefenseTechSpecInterface

    } // method handleDefense

    //************************************************************
    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for
     * ThreatModelInterface objects.  A threat model models a specific
     * type of threat.
     *
     */
    private void handleThreatModel()
    {
        ThreatModelInterface threat_ts = null;

        //--------------------
        //      ADD ThreatModelInterface
        //--------------------

        for ( Iterator iter = _threatModelSub.getAddedCollection().iterator();
              iter.hasNext() ; ) 
        {
            threat_ts = (ThreatModelInterface) iter.next(); 
         _intermediate_model.addThreatType( threat_ts );

        } // for ADD ThreatModelInterface

        //--------------------
        //      CHANGE ThreatModelInterface
        //--------------------

        for ( Iterator iter = _threatModelSub.getChangedCollection
                  ().iterator();  
          iter.hasNext() ; ) 
        {
            threat_ts = (ThreatModelInterface) iter.next(); 
         _intermediate_model.changeThreatType( threat_ts );

        } // for CHANGE ThreatModelInterface
        
        //--------------------
        //      REMOVE ThreatModelInterface
        //--------------------

        for ( Iterator iter = _threatModelSub.getRemovedCollection
                  ().iterator();  
          iter.hasNext() ; ) 
        {
            threat_ts = (ThreatModelInterface) iter.next(); 
         _intermediate_model.removeThreatType( threat_ts );

        } // for REMOVE ThreatModelInterface

    } // method handleThreatModel

    //************************************************************
    /**
     * Invoked via execute() in response to some Blackboard activity.
     *
     * This routine checks and handle BB events for
     * TimedDefenseDiagnosis objects.  
     *
     */
    private void handleDiagnosis()
    {
        TimedDefenseDiagnosis tdd = null;

        // Diagnoses are added to the blackboard. This is the only
	// consumer of diagnoses, so once believability has accessed
	// a TimedDefenseDiagnosis and done its updates, it needs to
	// remove the object from the blackboard.

        //--------------------
        //      ADD TimedDefenseDiagnosis
        //--------------------

	// Update the belief state for each asset
	for ( Iterator iter = _diagnosisSub.getAddedCollection().iterator();  
              iter.hasNext() ; ) 
        {
	    if (logger.isDebugEnabled() ) logger.debug ("TDD ADD");
            tdd = (TimedDefenseDiagnosis) iter.next(); 
	    if (logger.isDebugEnabled()) 
		logger.debug("ADDING TimedDefenseDiagnosis " + tdd.toString() );
	    try {
		// update the belief state, then remove the diagnosis input
		StateEstimation se = 
		    _intermediate_model.processDiagnosis( tdd );

		if ( se != null ) {
		    if (logger.isDebugEnabled()) 
			logger.debug("ADDING StateEstimation " + se.toString() );
		    publishAdd( se );
		}
		else {
		    if (logger.isDebugEnabled()) 
			logger.debug( " No valid diagnoses -- StateEstimation not added" );

		}
		// We do not want to remove the tdd because it is used to keep
		// track of some things downstream
		// publishRemove( tdd );
	    }
	    catch ( BelievabilityException be ) {
		logger.warn( "Received invalid TimedDefenseDiagnosis -- "
			     + be.getMessage() );
	    }
        } // for ADD TimedDefenseDiagnosis

        //--------------------
        //      CHANGE TimedDefenseDiagnosis
        //--------------------

	// TimedDefenseDiagnosis objects have changed.
	// We do nothing with this, as it is downstream of our plugin.
        for ( Iterator iter = _diagnosisSub.getChangedCollection
		  ().iterator();  
	      iter.hasNext() ; ) 
	    {
	    if (logger.isDebugEnabled() ) logger.debug ("TDD CHANGE ");
		tdd = (TimedDefenseDiagnosis) iter.next(); 
	    if (logger.isDebugEnabled()) 
		logger.debug("IGNORING change of TimedDefenseDiagnosis " 
			     + tdd.toString() );	    
	    } // for CHANGE TimedDefenseDiagnosis
        
        //--------------------
        //      REMOVE TimedDefenseDiagnosis
        //--------------------

	// The TimedDefenseDiagnosis has been removed successfully,
	// we do nothing with this since it is controlled elsewhere, and
	// we have already processed it.
        for ( Iterator iter = _diagnosisSub.getRemovedCollection
                  ().iterator();  
          iter.hasNext() ; ) 
        {
	    if (logger.isDebugEnabled() ) logger.debug ("TDD REMOVE");
            tdd = (TimedDefenseDiagnosis) iter.next(); 
	    if (logger.isDebugEnabled()) 
		logger.debug("IGNORING remove of TimedDefenseDiagnosis "
			     + tdd.toString() );
        } // for REMOVE TimedDefenseDiagnosis

    } // method handleDiagnosis


    //************************************************************
    /**
      * Demonstrates how to read in parameters passed in via
      * configuration files. Use/remove as needed.
      */
    private void getPluginParams() {
        
        //The 'logger' attribute is inherited. Use it to emit data for
        //debugging
        if (logger.isInfoEnabled() && getParameters().isEmpty()) 
            logger.error("plugin saw 0 parameters.");

        Iterator iter = getParameters().iterator (); 
        while (iter.hasNext()) {
             logger.debug("Parameter = " + (String)iter.next());
        }
    } // method getPluginParams

    //************************************************************
    /**
      * Called to ensure that the services we need get loaded.
      */
    private boolean haveServices() {

        if (eventService != null) return true;

        if (acquireServices()) 
        {
            if (logger.isDebugEnabled()) 
                logger.debug(".haveServices - acquiredServices.");

            ServiceBroker sb = getServiceBroker();
                
            eventService 
                = (EventService ) sb.getService( this, 
                                                 EventService.class, 
                                                 null ) ;      

            if (eventService == null) 
            {
                throw new RuntimeException("Unable to obtain EventService");
            }

            return true;
        }
        else if (logger.isDebugEnabled()) 
            logger.debug(".haveServices - did NOT acquire services.");

        return false;

    } // method haveServices

    //************************************************************

    // These are the subscriptions we need to monitor the BB activity.

    private IncrementalSubscription _threatModelSub;
    private IncrementalSubscription _assetSub;
    private IncrementalSubscription _defenseSub;
    private IncrementalSubscription _diagnosisSub;

    private IncrementalSubscription pluginControlSubscription;
    
    private EventService eventService = null;
    private static final Class[] requiredServices = {
        EventService.class
    };

    // This is the intermediate model that has all of the information
    // from the techspecs.
    //
    private IntermediateModel _intermediate_model = new IntermediateModel();

}  // class BelievabilityPlugin
