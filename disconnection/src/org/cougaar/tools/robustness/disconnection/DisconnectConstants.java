/*
 * DefenseConstants.java
 *
 * Created on August 10, 2003, 10:20 AM
 *
 
 * @author David Wells - OBJS 
 * 
 * <copyright>
 * 
 *  Copyright 2003-2004 Object Services and Consulting, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */

package org.cougaar.tools.robustness.disconnection;

public class DisconnectConstants {

    /** Creates new DefenseConstants */
    public DisconnectConstants() {}
    
    public static final String DEFENSE_NAME = "Disconnect";

    // Possible Diagnosis Values
    public final static String DISCONNECT_REQUEST = "Disconnect_Request";
    public final static String CONNECT_REQUEST = "Connect_Request";
    public final static String TARDY = "Tardy";
    public final static String DISCONNECTED = "Disconnected";
    public final static String CONNECTED = "Connected";

    // The Possible Actions
    public final static String ALLOW_DISCONNECT = "Allow_Disconnect";
    public final static String ALLOW_CONNECT = "Allow_Connect";
    public final static String AUTONOMOUS_RESTART = "Autonomous_Restart";

}
