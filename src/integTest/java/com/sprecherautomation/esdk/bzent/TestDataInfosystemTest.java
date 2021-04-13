package com.sprecherautomation.esdk.bzent;

import de.abas.erp.db.Deletable;
import de.abas.erp.db.infosystem.standard.st.Customers;
import de.abas.erp.db.schema.customer.CustomerEditor;
import de.abas.erp.db.schema.customer.ProspectEditor;
import de.abas.erp.db.schema.referencetypes.BusinessPartner;
import de.abas.erp.db.schema.referencetypes.BusinessPartnerEditor;
import de.abas.esdk.test.util.EsdkIntegTest;
import org.junit.*;

import static com.sprecherautomation.esdk.bzent.TestDataInfosystemTest.TestData.CUSTOMER;
import static com.sprecherautomation.esdk.bzent.TestDataInfosystemTest.TestData.PROSPECT;
import static com.sprecherautomation.esdk.bzent.ZipCodeEventHandlerTest.closeEditor;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestDataInfosystemTest extends EsdkIntegTest {

    @BeforeClass
    public static void createTestData() {
        for (final TestData testData : TestData.values()) {
            BusinessPartnerEditor businessPartnerEditor = ctx.newObject(testData.tClass);
            try {
                businessPartnerEditor.setSwd(testData.swd);
                businessPartnerEditor.setAddr(testData.address);
                businessPartnerEditor.setZipCode(testData.zipCode);
                businessPartnerEditor.setTown(testData.town);
                businessPartnerEditor.commit();
                testData.object = businessPartnerEditor;
            } finally {
                closeEditor(businessPartnerEditor);
            }
        }
    }

    @AfterClass
    public static void deleteTestData() {
        for (final TestData testData : TestData.values()) {
            if (testData.object instanceof Deletable) {
                ((Deletable) testData.object).delete();
            }
        }
    }

    private Customers customers = ctx.openInfosystem(Customers.class);

    @Before
    public void prepare() {
        customers.setBinteressent(true);
        customers.setBisach(false);
        customers.setBkunde(true);
        customers.setBsach(false);
    }

    @After
    public void tidyUp() {
        customers.abort();
    }

    @Test
    public void infosystemLKUCustomerTest() {
        customers.setKplz(CUSTOMER.zipCode);

        customers.invokeStart();

        assertThat(customers.table().getRowCount(), is(1));
        assertThat(customers.table().getRow(1).getTnum(), is(CUSTOMER.object));
        assertThat(customers.table().getRow(1).getPlz(), is(CUSTOMER.zipCode));
        assertThat(customers.table().getRow(1).getNort(), is(CUSTOMER.town));
    }

    @Test
    public void infosystemLKUProspectTest() {
        customers.setKplz(PROSPECT.zipCode);

        customers.invokeStart();

        assertThat(customers.table().getRowCount(), is(1));
        assertThat(customers.table().getRow(1).getTnum(), is(PROSPECT.object));
        assertThat(customers.table().getRow(1).getPlz(), is(PROSPECT.zipCode));
        assertThat(customers.table().getRow(1).getNort(), is(PROSPECT.town));
    }

    enum TestData {
        CUSTOMER(CustomerEditor.class, "TESTCUST", "Gartenstraße 67", "76135", "Karlsruhe"),
        PROSPECT(ProspectEditor.class, "TESTPROS", "Pfinztalstraße 105", "76227", "Karlsruhe");

        final Class<? extends BusinessPartnerEditor> tClass;
        final String swd;
        final String address;
        final String zipCode;
        final String town;
        BusinessPartner object;

        TestData(final Class<? extends BusinessPartnerEditor> tClass, final String swd, final String address, final String zipCode, final String town) {
            this.tClass = tClass;
            this.swd = swd;
            this.address = address;
            this.zipCode = zipCode;
            this.town = town;
        }
    }

}
