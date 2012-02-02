
package com.k_int.zcompare

import org.jzkit.z3950.util.ZEndpoint
import org.jzkit.z3950.gen.v3.Z39_50_APDU_1995.*
import org.jzkit.z3950.util.*
import org.jzkit.a2j.codec.util.OIDRegister
import org.jzkit.search.util.QueryModel.*
import org.jzkit.z3950.Z3950Exception
import org.jzkit.z3950.QueryModel.*

/**
 * Example Groovy class.
 */
class ZClient implements APDUListener {

  String host
  int port=210
  def authParams = [:]

  public static final int NO_CONNECTION = 1;
  public static final int CONNECTING = 2;
  public static final int CONNECTED = 3;
  public static final String NO_RS_NAME="default";
  int session_status = NO_CONNECTION;
  private HashMap responses = new HashMap();
  private Hashtable dbinfo = new Hashtable();

  private boolean supports_named_result_sets = true;

  OIDRegister reg = new org.jzkit.a2j.codec.util.OIDRegister("/a2j.properties")

  // These are the rules for converting friendly names to bib-1 use attributes
  InternalToType1ConversionRules rules = new org.jzkit.z3950.QueryModel.PropsBasedInternalToType1ConversionRules("/InternalToType1Rules.properties");

  ZEndpoint assoc = null;

  def show() {
    println 'Hello World'
    println "ZClient for z3950:${host}:${port}"
  }

  // public InitializeResponse_type connect(String hostname,
  //                                       int portnum,
  //                                       int auth_type,      // 0 = none, 1=Anonymous, 2=open, 3=idpass
  //                                       String principal,   // for open, the access string, for idpass, the id
  //                                       String group,       // group
  //                                       String credentials) // password
  def connect() {

    println("Connect");
    InitializeResponse_type retval = null;

    try {
      disconnect();

      System.out.println("Create listener & encoder");
      assoc = new ZEndpoint(reg);
      assoc.setHost(host);
      assoc.setPort(port);

      switch(authParams.method) {
        case 'userpass':
          println 'Configuring assoc for userpass authentication'
          assoc.setAuthType(3);
          assoc.setServiceUserPrincipal(authParams?.user);
          assoc.setServiceUserGroup(authParams?.group);
          assoc.setServiceUserCredentials(authParams?.pass);
          break;
        default:
          println 'Configuring assoc for no authentication'
          assoc.setAuthType(0);
          break;
      }


      // Convert incoming observer/observable notifications into PDU notifications.
      assoc.getPDUAnnouncer().addObserver( new GenericEventToOriginListenerAdapter(this) );

      // Look out for the init response
      PDUTypeAvailableSemaphore s = new PDUTypeAvailableSemaphore(PDU_type.initresponse_CID, assoc.getPDUAnnouncer() );

      assoc.start();

      try {
        s.waitForCondition(20000); // Wait up to 20 seconds for an init response
        retval = (InitializeResponse_type) s.the_pdu.o;
      }
      catch ( Exception e ) {
        println "Problem whilst waiting for init response...."
        e.printStackTrace()
      }
      finally {
        println "finally..."
        s.destroy();
      }
      println "completed"
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    return retval;
  }

  def disconnect() {
    if ( null != assoc ) {
      System.err.println("Closing existing listener");
      try {
        assoc.shutdown();
      }
      catch ( java.io.IOException ioe ) {
        ioe.printStackTrace();
      }
    }
  }

  public SearchResponse_type sendSearch(QueryModel query, 
                                        String refid,
                                        String setname) throws Z3950Exception, InvalidQueryException {
    return sendSearch(query,refid,setname,"F");
  }

  public SearchResponse_type sendSearch(QueryModel query, 
                                        String refid,
                                        String setname,
                                        String elements,
                                        String record_syntax,
                                        ArrayList db_names) throws Z3950Exception, InvalidQueryException {
    SearchResponse_type retval = null;

    if ( refid == null )
      refid = assoc.genRefid("Search");

    // ReferencedPDUAvaialableSemaphore s = new ReferencedPDUAvaialableSemaphore(refid, assoc.getPDUAnnouncer() );
    PDUTypeAvailableSemaphore s = new PDUTypeAvailableSemaphore(PDU_type.searchresponse_CID, assoc.getPDUAnnouncer() );

    try {

      Z3950QueryModel qry = null;

      // First thing we need to do is check that query is an instanceof Type1Query or to do a conversion...
      if ( query instanceof Z3950QueryModel )
        qry = (Z3950QueryModel) query;
      else
        qry = Type1QueryModelBuilder.buildFrom(query, "utf-8", reg, rules);
      
      assoc.sendSearchRequest(db_names, 
                              qry.toASNType(),
                              refid, 
                              0, 1, 1, true,   // ssub, lslb, mspn was 3,5,10
                              ( supports_named_result_sets == true ? setname : NO_RS_NAME ),   // Result set name
                              elements, 
                              elements, 
                              reg.oidByName(record_syntax));

      s.waitForCondition(20000); // Wait up to 20 seconds for a message of type SearchResponse
      retval = (SearchResponse_type) s.the_pdu.o;
    }
    catch ( java.io.IOException ioe )
    {
      // Problem with comms sending PDU...
      ioe.printStackTrace();
    }
    catch ( TimeoutExceededException tee )
    {
      tee.printStackTrace();
    }
    finally
    {
      s.destroy();
    }


    return retval;
  }

  /** 
   * Alternate sendSearch that simply passes along a search request PDU. Added for proxy server.
   */
  public SearchResponse_type sendSearch(PDU_type req) throws Z3950Exception, InvalidQueryException {
    SearchResponse_type retval = null;
    SearchRequest_type search_req = (SearchRequest_type)req.o;

    ReferencedPDUAvaialableSemaphore s = new ReferencedPDUAvaialableSemaphore(new String(search_req.referenceId), assoc.getPDUAnnouncer() );

    try
    {
      assoc.encodeAndSend(req);

      s.waitForCondition(20000); // Wait up to 20 seconds for a response
      retval = (SearchResponse_type) s.the_pdu.o;
    }
    catch ( java.io.IOException ioe )
    {
      ioe.printStackTrace();
    }
    catch ( TimeoutExceededException tee )
    {
      tee.printStackTrace();
    }
    finally
    {
      s.destroy();
    }

    return retval;
  }





  String toString() {
    def result = null
    if ( authParams?.method == 'userpass' )
      result = "z3950:${authParams.user}:${authParams.pass}@${host}:${port}"
    else
      result = "z3950:${host}:${port}"

    result
  }

  // Notification Handlers
  public void incomingAPDU(APDUEvent e){
    println "Incoming apdu"
  }

  public void incomingInitRequest(APDUEvent e){
    println("Incoming InitRequest");
    // Preparation for synchronous Retrieval API
    notifyAll();
  }

  public void incomingInitResponse(APDUEvent e) {
    println("Incoming InitResponse");

    InitializeResponse_type init_response = (InitializeResponse_type) (e.getPDU().o);
    responses.put(init_response.referenceId, init_response);
    session_status = CONNECTED;

    // log.fine("Incoming init response "+init_response.referenceId);

    if ( init_response.result.booleanValue() == true ) {
      if ( init_response.options.isSet(14) )
        println("Target supports named result sets");
      else
      {
        println("Target does not support named result sets");
        supports_named_result_sets = false;
      }
    }

    synchronized(this){
      notifyAll();
    }
  }

  public void incomingSearchRequest(APDUEvent e)  {
    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingSearchResponse(APDUEvent e) {
    SearchResponse_type search_response = (SearchResponse_type) e.getPDU().o;
    responses.put(search_response.referenceId, search_response);
    // log.fine("Incoming search response "+search_response.referenceId);

    synchronized(this){
      notifyAll();
    }
  }

  public void incomingPresentRequest(APDUEvent e) {
    // log.fine("Incoming PresentResponse");

    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingPresentResponse(APDUEvent e) {
    PresentResponse_type present_response = (PresentResponse_type) e.getPDU().o;
    responses.put(present_response.referenceId, present_response);

    synchronized(this){
      notifyAll();
    }
  }

  public void incomingDeleteResultSetRequest(APDUEvent e) {
    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingDeleteResultSetResponse(APDUEvent e) {
    // log.fine("Incoming DeleteResultSetResponse");

    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingAccessControlRequest(APDUEvent e) {
    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingAccessControlResponse(APDUEvent e) {
    // System.err.println("Incoming AccessControlResponse");

    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingResourceControlRequest(APDUEvent e) {
    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingResourceControlResponse(APDUEvent e) {
    // System.err.println("Incoming ResourceControlResponse");

    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingTriggerResourceControlRequest(APDUEvent e) {
    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingResourceReportRequest(APDUEvent e) {
    // System.err.println("Incoming ResourceReportResponse");

    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingResourceReportResponse(APDUEvent e) {
    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingScanRequest(APDUEvent e) {
    // System.err.println("Incoming ScanResponse");

    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingScanResponse(APDUEvent e) {
    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingSortRequest(APDUEvent e) {
    // System.err.println("Incoming SortResponse");
    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingSortResponse(APDUEvent e) {
    // Preparation for synchronous Retrieval API
    synchronized(this) {
      notifyAll();
    }
  }

  public void incomingSegmentRequest(APDUEvent e) {
    // System.err.println("Incoming SegmentResponse");

    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingExtendedServicesRequest(APDUEvent e) {
    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingExtendedServicesResponse(APDUEvent e) {
    // System.err.println("Incoming ExtendedServicesResponse");

    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

  public void incomingClose(APDUEvent e) {
    // System.err.println("Incoming close event: ");
    Close_type close_apdu = (Close_type) e.getPDU().o;
    // System.err.println("closeReason:"+close_apdu.closeReason);
    // System.err.println("diagnosticInformation:"+close_apdu.diagnosticInformation);

    // Preparation for synchronous Retrieval API
    synchronized(this){
      notifyAll();
    }
  }

}

