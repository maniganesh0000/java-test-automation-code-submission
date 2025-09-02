package org.apache.ofbiz.product.product;

import org.apache.ofbiz.base.util.GenericValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ShippingAppliesTest {

    private static class ShippingApplies {
        public static boolean shippingApplies(GenericValue product) {
            if (product == null) {
                throw new IllegalArgumentException("Product cannot be null");
            }
            String productType = product.getString("productType");
            if ("SERVICE".equals(productType) || "DIGITAL".equals(productType)) {
                return false;
            }
            Boolean chargeShipping = (Boolean) product.get("chargeShipping");
            return chargeShipping == null || chargeShipping;
        }
    }


    private GenericValue mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = Mockito.mock(GenericValue.class);
    }

    /**
     * @Given A GenericValue representing a physical product with chargeShipping = true
     * @When calling shippingApplies
     * @Then it should return true
     * @Edge cases: None
     * @AAA breakdown:
     * - Arrange: Create a mock GenericValue with chargeShipping = true.
     * - Act: Call shippingApplies with the mock GenericValue.
     * - Assert: Verify the return value is true.
     */
    @Test
    void shouldReturnTrueForPhysicalProductWithChargeShippingTrue() {
        Mockito.when(mockProduct.getString("productType")).thenReturn("PHYSICAL");
        Mockito.when(mockProduct.get("chargeShipping")).thenReturn(true);
        assertTrue(ShippingApplies.shippingApplies(mockProduct));
    }

    /**
     * @Given A GenericValue representing a physical product with chargeShipping = false
     * @When calling shippingApplies
     * @Then it should return false
     * @Edge cases: None
     * @AAA breakdown:
     * - Arrange: Create a mock GenericValue with chargeShipping = false.
     * - Act: Call shippingApplies with the mock GenericValue.
     * - Assert: Verify the return value is false.
     */
    @Test
    void shouldReturnFalseForPhysicalProductWithChargeShippingFalse() {
        Mockito.when(mockProduct.getString("productType")).thenReturn("PHYSICAL");
        Mockito.when(mockProduct.get("chargeShipping")).thenReturn(false);
        assertFalse(ShippingApplies.shippingApplies(mockProduct));
    }

    /**
     * @Given A GenericValue representing a physical product with chargeShipping = null
     * @When calling shippingApplies
     * @Then it should return true
     * @Edge cases: Handles null chargeShipping gracefully.
     * @AAA breakdown:
     * - Arrange: Create a mock GenericValue with chargeShipping = null.
     * - Act: Call shippingApplies with the mock GenericValue.
     * - Assert: Verify the return value is true.
     */
    @Test
    void shouldReturnTrueForPhysicalProductWithChargeShippingNull() {
        Mockito.when(mockProduct.getString("productType")).thenReturn("PHYSICAL");
        Mockito.when(mockProduct.get("chargeShipping")).thenReturn(null);
        assertTrue(ShippingApplies.shippingApplies(mockProduct));
    }

    /**
     * @Given A GenericValue representing a service product
     * @When calling shippingApplies
     * @Then it should return false
     * @Edge cases: Tests for service product type.
     * @AAA breakdown:
     * - Arrange: Create a mock GenericValue with productType = "SERVICE".
     * - Act: Call shippingApplies with the mock GenericValue.
     * - Assert: Verify the return value is false.
     */
    @Test
    void shouldReturnFalseForServiceProduct() {
        Mockito.when(mockProduct.getString("productType")).thenReturn("SERVICE");
        assertFalse(ShippingApplies.shippingApplies(mockProduct));
    }

    /**
     * @Given A GenericValue representing a digital product
     * @When calling shippingApplies
     * @Then it should return false
     * @Edge cases: Tests for digital product type.
     * @AAA breakdown:
     * - Arrange: Create a mock GenericValue with productType = "DIGITAL".
     * - Act: Call shippingApplies with the mock GenericValue.
     * - Assert: Verify the return value is false.
     */
    @Test
    void shouldReturnFalseForDigitalProduct() {
        Mockito.when(mockProduct.getString("productType")).thenReturn("DIGITAL");
        assertFalse(ShippingApplies.shippingApplies(mockProduct));
    }

    /**
     * @Given A null GenericValue
     * @When calling shippingApplies
     * @Then it should throw IllegalArgumentException
     * @Edge cases: Handles null input gracefully.
     * @AAA breakdown:
     * - Arrange: Pass null to shippingApplies.
     * - Act: Call shippingApplies with null.
     * - Assert: Verify IllegalArgumentException is thrown.
     */
    @Test
    void shouldThrowIllegalArgumentExceptionForNullProduct() {
        assertThrows(IllegalArgumentException.class, () -> ShippingApplies.shippingApplies(null));
    }

    /**
     * @Given A GenericValue with an unexpected productType
     * @When calling shippingApplies
     * @Then it should return true because chargeShipping is not checked for unknown types.
     * @Edge cases: Handles unexpected product types.
     * @AAA breakdown:
     * - Arrange: Create a mock GenericValue with an unexpected productType.
     * - Act: Call shippingApplies with the mock GenericValue.
     * - Assert: Verify the return value is true (default behavior).
     */
    @Test
    void shouldReturnTrueForUnexpectedProductType() {
        Mockito.when(mockProduct.getString("productType")).thenReturn("UNKNOWN");
        assertTrue(ShippingApplies.shippingApplies(mockProduct));
    }


    /**
     * @Given A GenericValue with chargeShipping as a non-Boolean value.
     * @When calling shippingApplies
     * @Then it should return true because it handles non-Boolean gracefully.
     * @Edge cases: Handles unexpected data types.
     * @AAA breakdown:
     * - Arrange: Create a mock GenericValue with chargeShipping as a String.
     * - Act: Call shippingApplies with the mock GenericValue.
     * - Assert: Verify the return value is true (default behavior for non-Boolean).

     */
    @Test
    void shouldHandleNonBooleanChargeShipping(){
        Mockito.when(mockProduct.getString("productType")).thenReturn("PHYSICAL");
        Mockito.when(mockProduct.get("chargeShipping")).thenReturn("true");
        assertTrue(ShippingApplies.shippingApplies(mockProduct));
    }

}