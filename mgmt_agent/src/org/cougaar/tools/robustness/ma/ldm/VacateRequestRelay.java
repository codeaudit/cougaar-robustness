package org.cougaar.tools.robustness.ma.ldm;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Iterator;
import java.io.ByteArrayOutputStream;
import org.cougaar.core.relay.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.persist.NotPersistable;


public class VacateRequestRelay implements Relay.Source, Relay.Target, XMLizable, NotPersistable
{
   private UID uid;
   private MessageAddress source;
   private transient Set targets;
   private MessageAddress target;
   private Object content;
   private Object response;
   private Properties props;

   /**
    * @param uid UID of this HbReq object
    * @param source MessageAddress of sending agent
    * @param targets Set of MessageAddresses of target agents
    * @param content HbReqContent to be sent with HbReq
    * @param response initial response
    */
   public VacateRequestRelay(UID uid,
                MessageAddress source,
                Set targets,
                Object content,
                Object response) {
                this.uid = uid;
                this.source = source;
                this.content = content;
                this.response = response;
                this.targets = ((targets == null) ? Collections.EMPTY_SET : new HashSet(targets));
    }

    // Unique Object implementation

    /**
     * UIDs are not permitted to be set except by constructor, so this
     * method throws an exception.
     *
     * @throws java.lang.RuntimeException
     **/
    public void setUID(UID uid) {
        throw new RuntimeException("Attempt to change UID");
    }

    /**
     * Get the UID for this object.
     *
     * @return org.cougaar.core.util.UID
     **/
    public UID getUID() {
      return uid;
    }

    // Relay.Source implementation

    /**
     * Get the addresses of the target agents to which this Relay should be sent.
     **/
    public Set getTargets() {
      return ((targets == null) ? Collections.EMPTY_SET : targets);
    }

    /**
     * Get an object representing the value of this Relay suitable for transmission.
     * A HbReqContent object is returned as an Object.
     **/
     public Object getContent() {
         return content;
     }

     private static final class SimpleRelayFactory
           implements TargetFactory, java.io.Serializable {

        public static final SimpleRelayFactory INSTANCE =
               new SimpleRelayFactory();

        private SimpleRelayFactory() {}

        /**
         * Convert the given content and related information into a Target
         * that will be published on the target's blackboard.
         **/
        public Relay.Target create(
                 UID uid,
                 MessageAddress source,
                 Object content,
                 Token token) {
             return new VacateRequestRelay(
                 uid, source, null, content, null);
             }

         private Object readResolve() {
               return INSTANCE;
         }
      };

   /**
    * Get a factory for creating the target.
    */
   public TargetFactory getTargetFactory() {
       return SimpleRelayFactory.INSTANCE;
   }

   /**
    * Update the source with the new response.
    */
   public int updateResponse(MessageAddress t, Object response) {
       // assert response != null
       if (!(response.equals(this.response))) {
            this.response = response;
            return Relay.RESPONSE_CHANGE;
       }
       return Relay.NO_CHANGE;
    }

    // Relay.Target implementation

    /**
     * Get the address of the Agent holding the Source copy of this Relay.
     */
    public MessageAddress getSource() {
        return source;
    }

    /**
     * Get the current Response for this target.
     */
    public Object getResponse() {
         return response;
    }

    /**
     * Update the target with the new content.
     */
    public int updateContent(Object content, Token token) {
        // assert content != null
        if (!(content.equals(this.content))) {
             this.content = content;
             return CONTENT_CHANGE;
        }
        return NO_CHANGE;
    }

    /**
     * XMLizable method for UI, other clients
     */
    public org.w3c.dom.Element getXML(org.w3c.dom.Document doc) {
        return XMLize.getPlanObjectXML(this, doc);
    }

    /**
     * Returns true if this object's UID equals the argument's UID.
     */
    public boolean equals(Object o) {
         if (o == this) {
              return true;
         } else if (!(o instanceof VacateRequestRelay)) {
              return false;
         } else {
            UID u = ((VacateRequestRelay) o).getUID();
            return uid.equals(u);
         }
     }

     /**
      * Returns a hash code based on this object's UID.
      */
     public int hashCode() {
         return uid.hashCode();
     }

     /**
      * Returns a String represention for this object.
      */
     public String toString() {
       return "VacateRequestRelay: " + content;
     }
}
