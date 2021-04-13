package com.sprecherautomation.esdk.bzent;

import de.abas.erp.axi.event.EventException;
import de.abas.erp.axi.screen.ScreenControl;
import de.abas.erp.axi2.EventHandlerRunner;
import de.abas.erp.axi2.annotation.ButtonEventHandler;
import de.abas.erp.axi2.annotation.EventHandler;
import de.abas.erp.axi2.event.ButtonEvent;
import de.abas.erp.axi2.type.ButtonEventType;
import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.infosystem.custom.owis.BZentrale;
import de.abas.erp.db.schema.company.Password;
import de.abas.erp.db.schema.company.PasswordEditor;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.jfop.rt.api.annotation.RunFopWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@EventHandler(head = BZentrale.class, row = BZentrale.Row.class)
@RunFopWith(EventHandlerRunner.class)
public class BZentraleEventHandler {

    @ButtonEventHandler(field="start", type = ButtonEventType.BEFORE)
    public void startBefore(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {

        if (!head.getYbzentucm().isEmpty() && !head.getYabteilung().isEmpty()) {
            throw new EventException("UCM Datei und Abteilung kann nicht gleichzeitig selektiert werden!");
        }

    }

    @ButtonEventHandler(field="start", type = ButtonEventType.AFTER)
    public void startAfter(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {

            SelectionBuilder<Password> sb = SelectionBuilder.create(Password.class);
            if (!head.getYbzentucm().isEmpty()) {
                //Selektion auf UCM-Datei
                sb.add(Conditions.eq(Password.META.grpFile1, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile2, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile3, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile4, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile5, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile6, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile7, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile8, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile9, head.getYbzentucm()));
                sb.add(Conditions.eq(Password.META.grpFile10, head.getYbzentucm()));
                sb.setTermConjunction(SelectionBuilder.Conjunction.OR);
            }

            if (!head.getYabteilung().isEmpty()) {
                //Selektion auf Abteilung
                sb.add(Conditions.eq(Password.META.deptUser, head.getYabteilung()));
            }

            head.table().clear();
            for (Password p : ctx.createQuery(sb.build())) {
                BZentrale.Row r = head.table().appendRow();
                r.setYtbzentpasswort(p);
                r.setYtbzentmitarbeiter(p.getEmployeePwdRef());

                fillGrpTableRowFields(p,r);
            }



    }

    private void fillGrpTableRowFields(Password p, BZentrale.Row r) {

        for (int a=1;a<=10;a++) {
            r.setYtbzentfcmdpwgrp(a,p.getGrpFile(a));
        }

    }

    @ButtonEventHandler(field="ybzentucmwaehlen", type = ButtonEventType.AFTER)
    public void ucmWaehlenAfter(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {

        File file = new File("win");
        ctx.out().println("Lokales ucm Verzeichnis "+file.getAbsolutePath());

        try {
            ctx.out().println("Folgende Dateien existieren:");
            Set<String> set = listFilesUsingDirectoryStream("win/ucm");

            for (String s : set) {
                ctx.out().println("- "+s);
            }

        } catch (IOException e) {
            ctx.out().println("Fehler");
        }


    }

    @ButtonEventHandler(field="yadducm", type = ButtonEventType.BEFORE)
    public void yadducmBefore(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {
        if (head.getYnewucm().isEmpty()) { throw new EventException("Keine UCM-Datei angegeben"); }
    }

    @ButtonEventHandler(field="yadducm", type = ButtonEventType.AFTER)
    public void yadducmAfter(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {

        for (BZentrale.Row r : head.getTableRows()) {

            int insertAt = head.getYnewucmpos();

            if (insertAt==0) {
                //Nächstes freies (leeres) Arbeitsgruppenmenü ermitteln
                for (int a = 1; a <= 10; a++) {
                    if (r.getYtbzentfcmdpwgrp(a).isEmpty()) {
                        insertAt = a;
                        break;
                    }
                }
            }

            if (insertAt>0) {
                Password p = r.getYtbzentpasswort();
                PasswordEditor pEditor = p.createEditor();
                try {
                    pEditor.open(EditorAction.UPDATE);

                    if (!pEditor.getGrpFile(insertAt).isEmpty()) {
                        //Arbeitsgruppenmenü bereits belegt --> umsortieren
                        reorderGrpFile(pEditor,insertAt);
                    }

                    pEditor.setGrpFile(insertAt,head.getYnewucm());
                    pEditor.setGrpPageShown(insertAt, true);
                    pEditor.commit();
                    fillGrpTableRowFields(p, r);
                } catch (CommandException e) {
                    pEditor.abort();
                }
            }

        }
    }

    private void reorderGrpFile(PasswordEditor pEditor, int startPos) {

        for (int a=10;a>startPos;a--) {
            pEditor.setGrpFile(a, pEditor.getGrpFile(a-1));
            pEditor.setGrpPageShown(a, pEditor.getGrpPageShown(a-1));
        }

        pEditor.setGrpFile(startPos,"");

    }

    @ButtonEventHandler(field="ydelucm", type = ButtonEventType.BEFORE)
    public void ydelucmBefore(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {
        if (head.getYnewucm().isEmpty()) { throw new EventException("Keine UCM-Datei angegeben"); }
    }

    @ButtonEventHandler(field="ydelucm", type = ButtonEventType.AFTER)
    public void ydelucmAfter(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {

        for (BZentrale.Row r : head.getTableRows()) {

            int deleteAt = 0;

            for (int a=1;a<=10;a++) {
                if (r.getYtbzentfcmdpwgrp(a).equalsIgnoreCase(head.getYnewucm())) {
                    deleteAt = a;
                    break;
                }
            }

            if (deleteAt>0) {
                Password p = r.getYtbzentpasswort();
                PasswordEditor pEditor = p.createEditor();
                try {
                    pEditor.open(EditorAction.UPDATE);
                    pEditor.setGrpFile(deleteAt,"");
                    pEditor.setGrpPageShown(deleteAt, false);
                    pEditor.commit();
                    fillGrpTableRowFields(p, r);
                } catch (CommandException e) {
                    pEditor.abort();
                }
            }

        }

    }

    @ButtonEventHandler(field="ygenallucm", type = ButtonEventType.BEFORE)
    public void ygenallucmBefore(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {
        throw new EventException("Diese Funktion ist noch nicht implementiert!");
    }

    @ButtonEventHandler(field="ygenallucm", type = ButtonEventType.AFTER)
    public void ygenallucmAfter(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {
        //TODO
    }

    public Set<String> listFilesUsingDirectoryStream(String dir) throws IOException {
        Set<String> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path.getFileName()
                            .toString());
                }
            }
        }
        return fileList;
    }

}
