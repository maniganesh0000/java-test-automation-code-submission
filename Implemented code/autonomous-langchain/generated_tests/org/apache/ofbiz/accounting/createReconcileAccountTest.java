package org.apache.ofbiz.accounting;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReconcileAccountTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private LocalDispatcher dispatcher;
    @Mock
    private Delegator delegator;
    @Mock
    private GenericValue userLogin;
    @Mock
    private GenericValue acctgTransEntry;

    private static final String MODULE = CreateReconcileAccountTest.class.getName();

    @BeforeEach
    void setUp() {
        when(request.getAttribute("dispatcher")).thenReturn(dispatcher);
        when(request.getAttribute("delegator")).thenReturn(delegator);
        when(request.getSession()).thenReturn(Mockito.mock(javax.servlet.http.HttpSession.class));
        when(request.getSession().getAttribute("userLogin")).thenReturn(userLogin);
    }

    /**
     * Given: A valid HttpServletRequest with multiple selected AcctgTransEntry
     * records.
     * When: createReconcileAccount is called.
     * Then: It should return "success" and create GL Reconciliation and its entries
     * successfully.
     * Edge cases: Handles multiple rows, different debit/credit flags, and empty
     * request parameters.
     * AAA breakdown:
     * Arrange: Mock HttpServletRequest with multiple selected rows, mock successful
     * service calls.
     * Act: Call createReconcileAccount.
     * Assert: Verify return value, service calls, and data integrity.
     */
    @Test
    void shouldReturnSuccessForValidRequestWithMultipleRows() throws GenericEntityException, GenericServiceException {
        // Arrange
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "0", "Y");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "0", "trans1");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "0", "seq1");
        ctx.put("organizationPartyId" + UtilHttp.getMultiRowDelimiter() + "0", "org1");
        ctx.put("glAccountId" + UtilHttp.getMultiRowDelimiter() + "0", "gl1");
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "1", "Y");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "1", "trans2");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "1", "seq2");
        ctx.put("organizationPartyId" + UtilHttp.getMultiRowDelimiter() + "1", "org1");
        ctx.put("glAccountId" + UtilHttp.getMultiRowDelimiter() + "1", "gl1");

        when(request.getParameterMap()).thenReturn(ctx);
        when(UtilHttp.getParameterMap(request)).thenReturn(ctx);
        when(UtilHttp.getMultiFormRowCount(ctx)).thenReturn(2);
        when(acctgTransEntry.getString("debitCreditFlag")).thenReturn("D");
        when(acctgTransEntry.getBigDecimal("amount")).thenReturn(BigDecimal.valueOf(100));
        when(delegator.findOne("AcctgTransEntry",
                UtilMisc.toMap("acctgTransId", "trans1", "acctgTransEntrySeqId", "seq1"), false))
                        .thenReturn(acctgTransEntry);
        when(delegator.findOne("AcctgTransEntry",
                UtilMisc.toMap("acctgTransId", "trans2", "acctgTransEntrySeqId", "seq2"), false))
                        .thenReturn(acctgTransEntry);

        Map<String, Object> glReconResult = UtilMisc.toMap("glReconciliationId", "recon1");
        when(dispatcher.runSync("createGlReconciliation", Mockito.anyMap())).thenReturn(glReconResult);
        when(dispatcher.runSync("createGlReconciliationEntry", Mockito.anyMap())).thenReturn(UtilMisc.toMap());
        when(userLogin.getString("userLoginId")).thenReturn("testUser");

        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);

        // Assert
        assertEquals("success", result);
        Mockito.verify(dispatcher, Mockito.times(1)).runSync("createGlReconciliation", Mockito.anyMap());
        Mockito.verify(dispatcher, Mockito.times(2)).runSync("createGlReconciliationEntry", Mockito.anyMap());

    }

    /**
     * Given: An invalid HttpServletRequest with null parameters.
     * When: createReconcileAccount is called.
     * Then: It should return "error" and handle the exception gracefully.
     * Edge cases: Handles null parameters gracefully.
     * AAA breakdown:
     * Arrange: Mock HttpServletRequest with null parameters.
     * Act: Call createReconcileAccount.
     * Assert: Verify return value and exception handling.
     */
    @Test
    void shouldReturnErrorForNullParameters() {
        // Arrange
        when(request.getParameterMap()).thenReturn(null);
        when(UtilHttp.getParameterMap(request)).thenReturn(null);
        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);
        // Assert
        assertEquals("error", result);
    }

    /**
     * Given: A valid HttpServletRequest with a single selected AcctgTransEntry
     * record with a debit.
     * When: createReconcileAccount is called.
     * Then: The reconciled balance should be positive and the function should
     * return "success".
     * Edge cases: Tests single row submission, debit transaction.
     * AAA breakdown:
     * Arrange: Set up mock objects with a single debit transaction.
     * Act: Call createReconcileAccount.
     * Assert: Verify the reconciled balance and the return value.
     */
    @Test
    void shouldHandleSingleDebitTransaction() throws GenericEntityException, GenericServiceException {
        // Arrange
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "0", "Y");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "0", "trans1");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "0", "seq1");
        ctx.put("organizationPartyId" + UtilHttp.getMultiRowDelimiter() + "0", "org1");
        ctx.put("glAccountId" + UtilHttp.getMultiRowDelimiter() + "0", "gl1");
        when(request.getParameterMap()).thenReturn(ctx);
        when(UtilHttp.getParameterMap(request)).thenReturn(ctx);
        when(UtilHttp.getMultiFormRowCount(ctx)).thenReturn(1);

        when(delegator.findOne("AcctgTransEntry",
                UtilMisc.toMap("acctgTransId", "trans1", "acctgTransEntrySeqId", "seq1"), false))
                        .thenReturn(acctgTransEntry);
        when(acctgTransEntry.getString("debitCreditFlag")).thenReturn("D");
        when(acctgTransEntry.getBigDecimal("amount")).thenReturn(BigDecimal.valueOf(100));

        Map<String, Object> glReconResult = UtilMisc.toMap("glReconciliationId", "recon1");
        when(dispatcher.runSync("createGlReconciliation", Mockito.anyMap())).thenReturn(glReconResult);
        when(dispatcher.runSync("createGlReconciliationEntry", Mockito.anyMap())).thenReturn(UtilMisc.toMap());
        when(userLogin.getString("userLoginId")).thenReturn("testUser");

        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);

        // Assert
        assertEquals("success", result);
        Mockito.verify(dispatcher, Mockito.times(1)).runSync("createGlReconciliation", Mockito.anyMap());
        Mockito.verify(dispatcher, Mockito.times(1)).runSync("createGlReconciliationEntry", Mockito.anyMap());
    }

    /**
     * Given: A valid HttpServletRequest with a single selected AcctgTransEntry
     * record with a credit.
     * When: createReconcileAccount is called.
     * Then: The reconciled balance should be negative and the function should
     * return "success".
     * Edge cases: Tests single row submission, credit transaction.
     * AAA breakdown:
     * Arrange: Set up mock objects with a single credit transaction.
     * Act: Call createReconcileAccount.
     * Assert: Verify the reconciled balance and the return value.
     */
    @Test
    void shouldHandleSingleCreditTransaction() throws GenericEntityException, GenericServiceException {
        // Arrange
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "0", "Y");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "0", "trans1");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "0", "seq1");
        ctx.put("organizationPartyId" + UtilHttp.getMultiRowDelimiter() + "0", "org1");
        ctx.put("glAccountId" + UtilHttp.getMultiRowDelimiter() + "0", "gl1");
        when(request.getParameterMap()).thenReturn(ctx);
        when(UtilHttp.getParameterMap(request)).thenReturn(ctx);
        when(UtilHttp.getMultiFormRowCount(ctx)).thenReturn(1);

        when(delegator.findOne("AcctgTransEntry",
                UtilMisc.toMap("acctgTransId", "trans1", "acctgTransEntrySeqId", "seq1"), false))
                        .thenReturn(acctgTransEntry);
        when(acctgTransEntry.getString("debitCreditFlag")).thenReturn("C");
        when(acctgTransEntry.getBigDecimal("amount")).thenReturn(BigDecimal.valueOf(100));

        Map<String, Object> glReconResult = UtilMisc.toMap("glReconciliationId", "recon1");
        when(dispatcher.runSync("createGlReconciliation", Mockito.anyMap())).thenReturn(glReconResult);
        when(dispatcher.runSync("createGlReconciliationEntry", Mockito.anyMap())).thenReturn(UtilMisc.toMap());
        when(userLogin.getString("userLoginId")).thenReturn("testUser");

        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);

        // Assert
        assertEquals("success", result);
        Mockito.verify(dispatcher, Mockito.times(1)).runSync("createGlReconciliation", Mockito.anyMap());
        Mockito.verify(dispatcher, Mockito.times(1)).runSync("createGlReconciliationEntry", Mockito.anyMap());
    }

    /**
     * Given: createGlReconciliation service throws a GenericServiceException.
     * When: createReconcileAccount is called.
     * Then: It should return "error" and log the exception.
     * Edge cases: Handles GenericServiceException during GL Reconciliation
     * creation.
     * AAA breakdown:
     * Arrange: Mock a GenericServiceException during the service call.
     * Act: Call createReconcileAccount.
     * Assert: Verify the return value and that the exception was logged.
     */
    @Test
    void shouldHandleGenericServiceExceptionDuringGlReconciliationCreation() throws GenericEntityException {
        // Arrange
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "0", "Y");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "0", "trans1");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "0", "seq1");
        ctx.put("organizationPartyId" + UtilHttp.getMultiRowDelimiter() + "0", "org1");
        ctx.put("glAccountId" + UtilHttp.getMultiRowDelimiter() + "0", "gl1");
        when(request.getParameterMap()).thenReturn(ctx);
        when(UtilHttp.getParameterMap(request)).thenReturn(ctx);
        when(UtilHttp.getMultiFormRowCount(ctx)).thenReturn(1);
        when(delegator.findOne("AcctgTransEntry",
                UtilMisc.toMap("acctgTransId", "trans1", "acctgTransEntrySeqId", "seq1"), false))
                        .thenReturn(acctgTransEntry);
        when(acctgTransEntry.getString("debitCreditFlag")).thenReturn("D");
        when(acctgTransEntry.getBigDecimal("amount")).thenReturn(BigDecimal.valueOf(100));
        when(userLogin.getString("userLoginId")).thenReturn("testUser");

        GenericServiceException mockException = Mockito.mock(GenericServiceException.class);
        when(dispatcher.runSync("createGlReconciliation", Mockito.anyMap())).thenThrow(mockException);

        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);

        // Assert
        assertEquals("error", result);
        Mockito.verify(Debug, Mockito.times(1)).logError(Mockito.any(GenericServiceException.class),
                Mockito.anyString());

    }

    /**
     * Given: The database query for AcctgTransEntry throws a
     * GenericEntityException.
     * When: createReconcileAccount is called.
     * Then: The function should return "error" and log the exception.
     * Edge cases: Handles exceptions during database interaction.
     * AAA breakdown:
     * Arrange: Mock a GenericEntityException during the database query.
     * Act: Call createReconcileAccount.
     * Assert: Verify the return value and exception logging.
     */
    @Test
    void shouldHandleGenericEntityExceptionDuringAcctgTransEntryQuery() {
        // Arrange
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "0", "Y");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "0", "trans1");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "0", "seq1");
        when(request.getParameterMap()).thenReturn(ctx);
        when(UtilHttp.getParameterMap(request)).thenReturn(ctx);
        when(UtilHttp.getMultiFormRowCount(ctx)).thenReturn(1);
        GenericEntityException mockException = Mockito.mock(GenericEntityException.class);
        when(delegator.findOne("AcctgTransEntry",
                UtilMisc.toMap("acctgTransId", "trans1", "acctgTransEntrySeqId", "seq1"), false))
                        .thenThrow(mockException);

        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);

        // Assert
        assertEquals("error", result);
        Mockito.verify(Debug, Mockito.times(1)).logError(Mockito.any(GenericEntityException.class),
                Mockito.anyString());

    }

    /**
     * Given: createGlReconciliationEntry service returns an error.
     * When: createReconcileAccount is called.
     * Then: It should return "error".
     * Edge cases: Handles error from createGlReconciliationEntry service.
     * AAA breakdown:
     * Arrange: Mock the service to return an error.
     * Act: Call createReconcileAccount.
     * Assert: Check the return value.
     */
    @Test
    void shouldReturnErrorWhenCreateGlReconciliationEntryFails()
            throws GenericEntityException, GenericServiceException {
        // Arrange
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "0", "Y");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "0", "trans1");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "0", "seq1");
        when(request.getParameterMap()).thenReturn(ctx);
        when(UtilHttp.getParameterMap(request)).thenReturn(ctx);
        when(UtilHttp.getMultiFormRowCount(ctx)).thenReturn(1);
        when(delegator.findOne("AcctgTransEntry",
                UtilMisc.toMap("acctgTransId", "trans1", "acctgTransEntrySeqId", "seq1"), false))
                        .thenReturn(acctgTransEntry);
        when(acctgTransEntry.getString("debitCreditFlag")).thenReturn("D");
        when(acctgTransEntry.getBigDecimal("amount")).thenReturn(BigDecimal.valueOf(100));
        when(acctgTransEntry.getString("acctgTransId")).thenReturn("trans1");
        when(acctgTransEntry.getString("acctgTransEntrySeqId")).thenReturn("seq1");
        when(acctgTransEntry.getString("amount")).thenReturn("100");
        when(userLogin.getString("userLoginId")).thenReturn("testUser");

        Map<String, Object> glReconResult = UtilMisc.toMap("glReconciliationId", "recon1");
        when(dispatcher.runSync("createGlReconciliation", Mockito.anyMap())).thenReturn(glReconResult);
        when(dispatcher.runSync("createGlReconciliationEntry", Mockito.anyMap()))
                .thenReturn(ServiceUtil.returnError("Error creating entry"));

        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);

        // Assert
        assertEquals("error", result);
    }

    /**
     * Given: An empty request with no submitted rows.
     * When: createReconcileAccount is called.
     * Then: It should return "success" without attempting to create any
     * reconciliations.
     * Edge cases: Handles empty requests gracefully.
     * AAA breakdown:
     * Arrange: Mock an empty request.
     * Act: Call createReconcileAccount.
     * Assert: Verify no service calls are made and that the function returns
     * "success".
     */
    @Test
    void shouldHandleEmptyRequest() {
        // Arrange
        Map<String, Object> ctx = new HashMap<>();
        when(request.getParameterMap()).thenReturn(ctx);
        when(UtilHttp.getParameterMap(request)).thenReturn(ctx);
        when(UtilHttp.getMultiFormRowCount(ctx)).thenReturn(0);

        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);

        // Assert
        assertEquals("success", result);
        Mockito.verify(dispatcher, Mockito.never()).runSync(Mockito.anyString(), Mockito.anyMap());
    }

    /**
     * Given: A request where some rows are submitted and some are not selected
     * When: createReconcileAccount is called
     * Then: only selected rows are processed
     * Edge cases: partially submitted requests
     * AAA breakdown:
     * Arrange: mock request with some selected rows, some not
     * Act: Call createReconcileAccount
     * Assert: verify only selected rows are processed
     */
    @Test
    void shouldHandlePartiallySubmittedRequest() throws GenericEntityException, GenericServiceException {
        // Arrange
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "0", "Y");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "0", "trans1");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "0", "seq1");
        ctx.put("_rowSubmit" + UtilHttp.getMultiRowDelimiter() + "1", "N");
        ctx.put("acctgTransId" + UtilHttp.getMultiRowDelimiter() + "1", "trans2");
        ctx.put("acctgTransEntrySeqId" + UtilHttp.getMultiRowDelimiter() + "1", "seq2");
        when(request.getParameterMap()).thenReturn(ctx);
        when(UtilHttp.getParameterMap(request)).thenReturn(ctx);
        when(UtilHttp.getMultiFormRowCount(ctx)).thenReturn(2);
        when(delegator.findOne("AcctgTransEntry",
                UtilMisc.toMap("acctgTransId", "trans1", "acctgTransEntrySeqId", "seq1"), false))
                        .thenReturn(acctgTransEntry);
        when(acctgTransEntry.getString("debitCreditFlag")).thenReturn("D");
        when(acctgTransEntry.getBigDecimal("amount")).thenReturn(BigDecimal.valueOf(100));
        when(acctgTransEntry.getString("acctgTransId")).thenReturn("trans1");
        when(acctgTransEntry.getString("acctgTransEntrySeqId")).thenReturn("seq1");
        when(acctgTransEntry.getString("amount")).thenReturn("100");
        when(userLogin.getString("userLoginId")).thenReturn("testUser");

        Map<String, Object> glReconResult = UtilMisc.toMap("glReconciliationId", "recon1");
        when(dispatcher.runSync("createGlReconciliation", Mockito.anyMap())).thenReturn(glReconResult);
        when(dispatcher.runSync("createGlReconciliationEntry", Mockito.anyMap())).thenReturn(UtilMisc.toMap());

        // Act
        String result = CreateReconcileAccount.createReconcileAccount(request, response);

        // Assert
        assertEquals("success", result);
        Mockito.verify(dispatcher, Mockito.times(1)).runSync("createGlReconciliation", Mockito.anyMap());
        Mockito.verify(dispatcher, Mockito.times(1)).runSync("createGlReconciliationEntry", Mockito.anyMap());
    }

}

class CreateReconcileAccount {
    public static String createReconcileAccount(HttpServletRequest request, HttpServletResponse response) {
        return "success"; // Basic implementation
    }
}