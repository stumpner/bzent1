package com.sprecherautomation.esdk.bzent;

import de.abas.erp.axi.event.EventException;
import de.abas.erp.axi2.EventHandlerRunner;
import de.abas.erp.axi2.annotation.EventHandler;
import de.abas.erp.axi2.annotation.FieldEventHandler;
import de.abas.erp.axi2.type.FieldEventType;
import de.abas.erp.db.schema.customer.CustomerEditor;
import de.abas.erp.jfop.rt.api.annotation.RunFopWith;

@EventHandler(head = CustomerEditor.class)
@RunFopWith(EventHandlerRunner.class)
public class ZipCodeEventHandler {

    @FieldEventHandler(field = "zipCode", type = FieldEventType.VALIDATION)
    public void zipCodeValidation(CustomerEditor head) throws EventException {
        String zipCode = head.getZipCode();
        ZipCodeValidator validator = new ZipCodeValidator();
        if (zipCode.isEmpty() || validator.isGermanZipCode(zipCode)) {
            return;
        }
        throw new EventException(String.format("invalid German zipCode `%s`", zipCode), 1);
    }
}
