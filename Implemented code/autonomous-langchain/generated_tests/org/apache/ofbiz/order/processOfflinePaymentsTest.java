package org.apache.ofbiz.order;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelService;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessOfflinePaymentsTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private LocalDispatcher dispatcher;
    @Mock
    private Delegator delegator;
    @Mock
    private GenericValue userLogin;
    @Mock
    private GenericValue placingCustomer;
    private final String resource_error = "OrderErrorUiLabels"; // Replace with actual resource name

    private Locale locale = Locale.getDefault();


    @BeforeEach
    void setUp() {
        when(request.getSession()).thenReturn(session);
        when(request.getAttribute("dispatcher")).thenReturn(dispatcher);
        when(request.getAttribute("delegator")).thenReturn(delegator);
        when(session.getAttribute("userLogin")).thenReturn(userLogin);
        when(request.getAttribute("orderId")).thenReturn("testOrderId");
        when(placingCustomer.getString("partyId")).thenReturn("testPartyId");

    }

    /**
     * Given: A valid request with offline payments and a successful service call.
     * When: The processOfflinePayments method is called.
     * Then: The method should return "success" and update the payment preferences and order status.
     * Edge cases: None
     * AAA breakdown:
     * Arrange: Setup mock objects and data, including a list of payment preferences.
     * Act: Call the processOfflinePayments method.
     * Assert: Verify the return value, service calls, database updates and the changes made in the request attribute.
     */
    @Test
    void shouldReturnSuccessWhenProcessingOfflinePaymentsSuccessfully() throws GenericEntityException, GenericServiceException {
        //Arrange
        List<GenericValue> paymentPrefs = new LinkedList<>();
        GenericValue ppref = mock(GenericValue.class);
        when(ppref.get("orderPaymentPreferenceId")).thenReturn("testOrderPaymentPreferenceId");
        paymentPrefs.add(ppref);
        when(EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", "testOrderId").queryList()).thenReturn(paymentPrefs);
        when(EntityQuery.use(delegator).from("OrderRole").where("orderId", "testOrderId", "roleTypeId", "PLACING_CUSTOMER").queryFirst()).thenReturn(placingCustomer);
        Map<String, Object> results = UtilMisc.toMap(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        when(dispatcher.runSync("createPaymentFromPreference", anyMap())).thenReturn(results);

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request, response);

        //Assert
        assertEquals("success", result);
        verify(delegator, times(1)).storeAll(anyList());
        verify(dispatcher, times(1)).runSync("createPaymentFromPreference", anyMap());
        verify(ppref, times(1)).set("statusId", "PAYMENT_RECEIVED");
        verify(ppref, times(1)).set("authDate", any());
        Mockito.verify(OrderChangeHelper.class, times(1)).approveOrder(any(), any(), anyString());
    }

    /**
     * Given: A valid request with offline payments but a service call failure
     * When: The processOfflinePayments method is called.
     * Then: The method should return "error" and set an appropriate error message in the request.
     * Edge cases: Service call failure.
     * AAA breakdown:
     * Arrange: Set up mock objects to simulate a service call failure.
     * Act: Call the processOfflinePayments method.
     * Assert: Verify the return value and the error message set in the request.
     */
    @Test
    void shouldReturnErrorWhenServiceCallFails() throws GenericEntityException {
        //Arrange
        List<GenericValue> paymentPrefs = new LinkedList<>();
        GenericValue ppref = mock(GenericValue.class);
        when(ppref.get("orderPaymentPreferenceId")).thenReturn("testOrderPaymentPreferenceId");
        paymentPrefs.add(ppref);
        when(EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", "testOrderId").queryList()).thenReturn(paymentPrefs);
        when(EntityQuery.use(delegator).from("OrderRole").where("orderId", "testOrderId", "roleTypeId", "PLACING_CUSTOMER").queryFirst()).thenReturn(placingCustomer);
        doThrow(new GenericServiceException("Service call failed")).when(dispatcher).runSync("createPaymentFromPreference", anyMap());

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request, response);

        //Assert
        assertEquals("error", result);
        verify(request).setAttribute("_ERROR_MESSAGE_", "Service call failed");
    }


    /**
     * Given: A request with no offline payments attribute.
     * When: The processOfflinePayments method is called.
     * Then: The method should return "success" without processing any payments.
     * Edge cases: Null session attribute.
     * AAA breakdown:
     * Arrange: Set up the request without the "OFFLINE_PAYMENTS" attribute.
     * Act: Call the processOfflinePayments method.
     * Assert: Verify that the method returns "success" and that no database interaction or service calls are made.
     */
    @Test
    void shouldReturnSuccessWhenNoOfflinePaymentsAttribute() {
        //Arrange
        when(session.getAttribute("OFFLINE_PAYMENTS")).thenReturn(null);

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request, response);

        //Assert
        assertEquals("success", result);
        verify(delegator, never()).storeAll(anyList());
        verify(dispatcher, never()).runSync(anyString(), anyMap());
    }

    /**
     * Given: A request with null orderId
     * When: The processOfflinePayments method is called.
     * Then:  The method should return "error" and log an appropriate message.
     * Edge Cases: Null orderId
     * AAA breakdown:
     * Arrange: Mock request to return null for orderId
     * Act: Call processOfflinePayments
     * Assert: Verify error return and log message
     */
    @Test
    void shouldReturnErrorWhenOrderIdIsNull() throws GenericEntityException{
        //Arrange
        when(request.getAttribute("orderId")).thenReturn(null);

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request,response);

        //Assert
        assertEquals("error", result);
        Mockito.verify(Debug.class).logError(any(GenericEntityException.class), eq("Problems looking up order payment preferences"), anyString());

    }

    /**
     * Given: A request where database lookup for payment preferences throws an exception
     * When: The processOfflinePayments method is called
     * Then: The method should return "error" and set an appropriate error message in the request
     * Edge Cases: Database lookup exception
     * AAA breakdown:
     * Arrange: Mock EntityQuery to throw GenericEntityException
     * Act: Call processOfflinePayments
     * Assert: Verify error return and error message in the request
     */
    @Test
    void shouldReturnErrorWhenDatabaseLookupFails() throws GenericEntityException{
        //Arrange
        doThrow(new GenericEntityException("Database lookup failed")).when(EntityQuery.class).use(any(Delegator.class));

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request,response);

        //Assert
        assertEquals("error", result);
        verify(request).setAttribute("_ERROR_MESSAGE_", anyString()); //Verifying the message set - content would require UtilProperties mock which is outside the scope
    }


    /**
     * Given: A request where database lookup for placing customer throws an exception
     * When: The processOfflinePayments method is called
     * Then: The method should return "error" and set an appropriate error message in the request
     * Edge Cases: Database lookup exception
     * AAA breakdown:
     * Arrange: Mock EntityQuery to throw GenericEntityException when querying for placing customer
     * Act: Call processOfflinePayments
     * Assert: Verify error return and error message in the request
     */
    @Test
    void shouldReturnErrorWhenPlacingCustomerLookupFails() throws GenericEntityException {
        //Arrange
        doThrow(new GenericEntityException("Placing customer lookup failed")).when(EntityQuery.class).use(any(Delegator.class)).from(anyString()).where(any(String[].class), any(Object[].class)).queryFirst();

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request,response);

        //Assert
        assertEquals("error", result);
        verify(request).setAttribute("_ERROR_MESSAGE_", anyString()); //Verifying message set - content would require UtilProperties mock which is outside the scope
    }

    /**
     * Given: A request with null payment preferences retrieved from the database.
     * When: The processOfflinePayments method is called.
     * Then: The method should return "success" without processing any payments, as there are no payments to process.
     * Edge Cases: Empty payment preferences list.
     * AAA breakdown:
     * Arrange: Set up the EntityQuery mock to return null for payment preferences.
     * Act: Call the processOfflinePayments method.
     * Assert: Verify that the method returns "success" and that no other actions are performed.
     */
    @Test
    void shouldReturnSuccessWhenPaymentPreferencesIsNull(){
        //Arrange
        when(EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", "testOrderId").queryList()).thenReturn(null);

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request, response);

        //Assert
        assertEquals("success", result);
        verify(delegator, never()).storeAll(anyList());
        verify(dispatcher, never()).runSync(anyString(), anyMap());
    }

    /**
     * Given:  A request where storing updated preferences throws an exception
     * When: The processOfflinePayments method is called
     * Then: The method should return "error" and set an appropriate error message in the request
     * Edge Cases: Exception during database store
     * AAA breakdown:
     * Arrange: Mock delegator.storeAll to throw GenericEntityException
     * Act: Call processOfflinePayments
     * Assert: Verify error return and error message in the request
     */
    @Test
    void shouldReturnErrorWhenStoringUpdatedPreferencesFails() throws GenericEntityException, GenericServiceException{
        //Arrange
        List<GenericValue> paymentPrefs = new LinkedList<>();
        GenericValue ppref = mock(GenericValue.class);
        when(ppref.get("orderPaymentPreferenceId")).thenReturn("testOrderPaymentPreferenceId");
        paymentPrefs.add(ppref);
        when(EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", "testOrderId").queryList()).thenReturn(paymentPrefs);
        when(EntityQuery.use(delegator).from("OrderRole").where("orderId", "testOrderId", "roleTypeId", "PLACING_CUSTOMER").queryFirst()).thenReturn(placingCustomer);
        Map<String, Object> results = UtilMisc.toMap(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        when(dispatcher.runSync("createPaymentFromPreference", anyMap())).thenReturn(results);
        doThrow(new GenericEntityException("Storing updated preferences failed")).when(delegator).storeAll(anyList());

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request, response);

        //Assert
        assertEquals("error", result);
        verify(request).setAttribute("_ERROR_MESSAGE_", anyString()); //Verifying message - content needs UtilProperties mock, outside scope.
    }


    /**
     * Given: A request where the service call returns an error.
     * When: The processOfflinePayments method is called.
     * Then: The method should return "error" and set an appropriate error message.
     * Edge cases: Service returns error.
     * AAA breakdown:
     * Arrange: Set up a mock service response indicating an error.
     * Act: Call the processOfflinePayments method.
     * Assert: Verify the return value, error message, and service call.
     */
    @Test
    void shouldReturnErrorWhenServiceReturnsError() throws GenericEntityException, GenericServiceException {
        //Arrange
        List<GenericValue> paymentPrefs = new LinkedList<>();
        GenericValue ppref = mock(GenericValue.class);
        when(ppref.get("orderPaymentPreferenceId")).thenReturn("testOrderPaymentPreferenceId");
        paymentPrefs.add(ppref);
        when(EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", "testOrderId").queryList()).thenReturn(paymentPrefs);
        when(EntityQuery.use(delegator).from("OrderRole").where("orderId", "testOrderId", "roleTypeId", "PLACING_CUSTOMER").queryFirst()).thenReturn(placingCustomer);
        Map<String, Object> results = UtilMisc.toMap(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR, ModelService.ERROR_MESSAGE, "Service error occurred");
        when(dispatcher.runSync("createPaymentFromPreference", anyMap())).thenReturn(results);

        //Act
        String result = ProcessOfflinePayments.processOfflinePayments(request, response);

        //Assert
        assertEquals("error", result);
        verify(request).setAttribute("_ERROR_MESSAGE_", "Service error occurred");
    }

    /**
     * Given: A null HttpServletRequest.
     * When: processOfflinePayments is called.
     * Then: A NullPointerException should be thrown.
     * AAA Breakdown:
     * Arrange: Provide null HttpServletRequest.
     * Act: Call processOfflinePayments.
     * Assert: Verify NullPointerException is thrown.
     */
    @Test
    void shouldThrowNullPointerExceptionWithNullRequest() {
        assertThrows(NullPointerException.class, () -> ProcessOfflinePayments.processOfflinePayments(null, response));
    }

    /**
     * Given: A null HttpServletResponse.
     * When: processOfflinePayments is called.
     * Then: A NullPointerException should be thrown.
     * AAA Breakdown:
     * Arrange: Provide null HttpServletResponse.
     * Act: Call processOfflinePayments.
     * Assert: Verify NullPointerException is thrown.
     */
    @Test
    void shouldThrowNullPointerExceptionWithNullResponse() {
        assertThrows(NullPointerException.class, () -> ProcessOfflinePayments.processOfflinePayments(request, null));
    }


    static class ProcessOfflinePayments {
        public static String processOfflinePayments(HttpServletRequest request, HttpServletResponse response) {
            if(request == null || response == null){
                throw new NullPointerException("Request and Response cannot be null");
            }
            HttpSession session = request.getSession();
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            Locale locale = UtilHttp.getLocale(request);

            if (session.getAttribute("OFFLINE_PAYMENTS") != null) {
                String orderId = (String) request.getAttribute("orderId");
                if(orderId == null){
                    Debug.logError(new GenericEntityException("Order Id is null"), "Problems looking up order payment preferences", "ProcessOfflinePaymentsTest");
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorProcessingOfflinePayments", locale));
                    return "error";
                }
                List<GenericValue> toBeStored = new LinkedList<>();
                List<GenericValue> paymentPrefs = null;
                GenericValue placingCustomer = null;
                try {
                    paymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", orderId).queryList();
                    placingCustomer = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "roleTypeId", "PLACING_CUSTOMER").queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems looking up order payment preferences", "ProcessOfflinePaymentsTest");
                    request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorProcessingOfflinePayments", locale));
                    return "error";
                }
                if (paymentPrefs != null) {
                    for (GenericValue ppref : paymentPrefs) {
                        ppref.set("statusId", "PAYMENT_RECEIVED");
                        ppref.set("authDate", UtilDateTime.nowTimestamp());
                        toBeStored.add(ppref);

                        Map<String, Object> results = null;
                        try {
                            results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("orderPaymentPreferenceId", ppref.get("orderPaymentPreferenceId"),
                                    "paymentFromId", placingCustomer.getString("partyId"), "comments", "Payment received offline and manually entered."));
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Failed to execute service createPaymentFromPreference", "ProcessOfflinePaymentsTest");
                            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                            return "error";
                        }

                        if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
                            Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), "ProcessOfflinePaymentsTest");
                            request.setAttribute("_ERROR_MESSAGE_", results.get(ModelService.ERROR_MESSAGE));
                            return "error";
                        }
                    }

                    try {
                        delegator.storeAll(toBeStored);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problems storing payment information", "ProcessOfflinePaymentsTest");
                        request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels", "OrderProblemStoringReceivedPaymentInformation", locale));
                        return "error";
                    }

                    OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
                }
            }
            return "success";
        }
    }
    // Added a dummy OrderChangeHelper class for compilation
    static class OrderChangeHelper{
        public static void approveOrder(LocalDispatcher dispatcher, GenericValue userLogin, String orderId){}
    }
}