package com.sprecherautomation.esdk.bzent;

import de.abas.eks.jfop.remote.FO;
import de.abas.erp.api.gui.MenuBuilder;
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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                sb.add(Conditions.eq(Password.META.pwdInactive, false));
            }

            head.table().clear();
            for (Password p : ctx.createQuery(sb.build())) {
                BZentrale.Row r = head.table().appendRow();
                r.setYtbzentpasswort(p);
                r.setYtbzentmitarbeiter(p.getEmployeePwdRef());
                r.setYtabteilung(p.getDeptUser());
                r.setYtinaktiv(p.getPwdInactive());

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

        try {
            MenuBuilder<String> mb = new MenuBuilder<>(ctx, "wählen");
            List<String> set = listFilesUsingDirectoryStream("win/ucm");

            for (String s : set) {
                mb.addItem(s,s);
            }

            String result = mb.show();
            if (result.toUpperCase().endsWith(".UCM")) {
                result = result.substring(0,result.length()-4);
            }
            head.setYbzentucm(result);

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
        //throw new EventException("Diese Funktion ist noch nicht implementiert!");
    }

    @ButtonEventHandler(field="ygenallucm", type = ButtonEventType.AFTER)
    public void ygenallucmAfter(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {

        ctx.out().println("all.ucm wird generiert");

        try {
            List<String> set = listFilesUsingDirectoryStream("win/ucm");

            File allFile = new File("win/ucm/ALL.ucm");
            if (allFile.exists()) { allFile.delete(); }
            FileOutputStream allFs = new FileOutputStream(allFile);
            OutputStreamWriter allOw = new OutputStreamWriter(allFs, "UTF-16LE");
            BufferedWriter allWriter = new BufferedWriter(allOw);

            int ic = 65279; //Spezielles Steuerzeichen auf Position 1 wird erwartet von abas
            char beginChar = (char)ic;

            allWriter.write(beginChar+"TITLE TEXT \"ALL\";");
            allWriter.newLine();

            for (String s : set) {

                ctx.out().println("File: "+s);

                if (!s.equalsIgnoreCase("ALL.ucm")) {

                    allWriter.write("TREENODE TEXT \""+s+"\";");
                    allWriter.newLine();

                    //ucm File ist codiert in UTF-16LE? vielleicht nur wegen Docker?
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("win/ucm/" + s), "UTF-16LE"));

                    String fileContent = new String();

                    int zeileNr = 0;
                    String zeile = null;
                    while ((zeile = br.readLine()) != null) {
                        zeileNr = zeileNr + 1;
                        fileContent = fileContent + zeile;

                        if (!zeile.contains("TITLE TEXT")) {

                            if (zeileNr==1) { //Erste Zeile im File, das Steuerzeichen \65279 auf pos 1 entfernen
                                allWriter.write(zeile.substring(1));
                            } else {
                                allWriter.write(zeile);
                            }
                            allWriter.newLine();

                        }

                    }

                    br.close();

                    allWriter.write("END");
                    allWriter.newLine();

                }

            }

            allWriter.close();

        } catch (IOException e) {
            ctx.out().println("Fehler "+e.getMessage());
        }

    }

    public List<String> listFilesUsingDirectoryStream(String dir) throws IOException {
        Set<String> fileSet = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileSet.add(path.getFileName()
                            .toString());
                }
            }
        }

        //Sortieren
        List<String> fileList = fileSet.stream().collect(Collectors.toList());
        Collections.sort(fileList, (o1,o2) -> o1.compareTo(o2));
        return fileList;
    }

    @ButtonEventHandler(field="yabteilungwaehlen", type = ButtonEventType.AFTER)
    public void yabteilungwaehlenAfter(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {

        MenuBuilder<String> mb = new MenuBuilder<>(ctx, "wählen");

        SelectionBuilder<Password> sb = SelectionBuilder.create(Password.class);

        HashMap<String,String> abtSet = new HashMap<String,String>();

        for (Password p : ctx.createQuery(sb.build())) {
            if (!p.getDeptUser().isEmpty()) {
                abtSet.put(p.getDeptUser(), "X");
            }
        }

        for (String a : abtSet.keySet()) {
            mb.addItem(a,a);
        }

        String s = mb.show();
        head.setYabteilung(s);

    }

    @ButtonEventHandler(field="ykommandosuchen", type = ButtonEventType.AFTER)
    public void ykommandosuchenAfter(ButtonEvent event, ScreenControl screenControl, DbContext ctx, BZentrale head) throws EventException {

        String k = FO.lesen(new String[] {"Kommando in UCM Datei suchen","Nach welchem Kommando soll in den ucm-Dateien gesucht werden?"});

        ctx.out().println("Gesucht wird das Kommando: "+k+" in allen UCM Dateien");

        try {
            List<String> set = listFilesUsingDirectoryStream("win/ucm");

            for (String s : set) {

                //ucm File ist codiert in UTF-16LE? vielleicht nur wegen Docker?
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("win/ucm/"+s), "UTF-16LE"));

                String fileContent = new String();

                String zeile = null;
                while ((zeile = br.readLine()) != null) {
                    fileContent = fileContent+zeile;

                    if (zeile.contains(k)) {
                        ctx.out().println(s+": "+zeile);
                    }
                }

            }

        } catch (IOException e) {
            ctx.out().println("Fehler "+e.getMessage());
        }

    }

}
