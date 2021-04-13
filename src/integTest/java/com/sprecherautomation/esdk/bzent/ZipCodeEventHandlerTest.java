package com.sprecherautomation.esdk.bzent;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorObject;
import de.abas.erp.db.schema.customer.CustomerEditor;
import de.abas.esdk.test.util.DbContextProvider;
import org.junit.ClassRule;
import org.junit.Test;

public class ZipCodeEventHandlerTest {

    @ClassRule
    public static DbContextProvider ctxProvider = new DbContextProvider();
    private DbContext ctx = ctxProvider.ctx;

    @Test(expected = RuntimeException.class)
    public void cantEnterInvalidZipCode() {
        CustomerEditor customerEditor = ctx.newObject(CustomerEditor.class);
        try {
            customerEditor.setZipCode("0815");
        } finally {
            closeEditor(customerEditor);
        }
    }

    @Test
    public void canEnterValidZipCode() {
        CustomerEditor customerEditor = ctx.newObject(CustomerEditor.class);
        try {
            customerEditor.setZipCode("71245");
        } finally {
            closeEditor(customerEditor);
        }
    }

    static void closeEditor(EditorObject editor) {
        if (editor != null && editor.active()) {
            editor.abort();
        }
    }

}
