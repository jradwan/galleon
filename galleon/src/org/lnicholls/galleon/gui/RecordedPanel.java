package org.lnicholls.galleon.gui;

/*
 * Copyright (C) 2005 Leon Nicholls
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 * See the file "COPYING" for more details.
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.*;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.togo.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Owner
 * 
 * TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code
 * Templates
 */
public class RecordedPanel extends JPanel implements ProgressListener {

    private static Logger log = Logger.getLogger(RecordedPanel.class.getName());

    private static class ColumnData {
        public String mTitle;

        public int mWidth;

        public int mAlignment;

        public ColumnData(String title, int width, int alignment) {
            mTitle = title;
            mWidth = width;
            mAlignment = alignment;
        }
    }

    public class RecordedThread extends Thread {

        public RecordedThread(RecordedPanel recordedPanel) {
            super("RecordedThread");
            mRecordedPanel = recordedPanel;
        }

        public void run() {
            while (true) {
                try {
                    ArrayList tivos = Galleon.getServerConfiguration().getTiVos();
                    if (tivos.size() > 0) {
                        ToGoList togoList = new ToGoList();
                        
                        ArrayList downloads = togoList.load(Show.STATUS_DOWNLOADING | Show.STATUS_PENDING | Show.STATUS_USER_CANCELLED | Show.STATUS_RECORDED | Show.STATUS_RECORDING | Show.STATUS_INCOMPLETE | Show.STATUS_USER_SELECTED);
                        if (downloads.size()==0)
                        {
                            Galleon.getToGo().getRecordings(tivos,RecordedPanel.this, mRecorded);
                            mTitleField.setText(" ");
                            mDescriptionField.setText(" ");
                            if (log.isDebugEnabled())
                                log.debug("mRecorded=" + mRecorded.size());
    
                            ArrayList downloaded = togoList.load();
                            
                            Iterator recordedIterator = mRecorded.iterator();
                            while (recordedIterator.hasNext()) {
                                Show next = (Show) recordedIterator.next();
                            
                                boolean found = false;
                                Iterator iterator = downloaded.iterator();
                                while (iterator.hasNext()) {
                                    Show show = (Show) iterator.next();
                                    if (show.equals(next)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found)
                                {
                                    downloaded.add(next);
                                }
                            }
                            togoList.save(downloaded);
                            
                            Galleon.getToGo().applyRules();
                            
                            mRecorded = togoList.load(Show.STATUS_DOWNLOADING | Show.STATUS_PENDING | Show.STATUS_USER_CANCELLED | Show.STATUS_RECORDED | Show.STATUS_RECORDING | Show.STATUS_INCOMPLETE);
                        }
                        else
                        {
                            mRecorded = downloads; 
                        }
                        
                        Collections.sort(mRecorded, new ShowComparator(3, false));
                        ShowTableData model = (ShowTableData) mTable.getModel();
                        model.fireTableDataChanged();
                        if (model.getRowCount() > 0)
                            mTable.setRowSelectionInterval(0, 0);
                        mRecordedPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));                        
                    } 
                    sleep(1000*60);
                } catch (Throwable ex) {
                    //HMOTools.logException(RecordedThread.class, ex);
                }
            }
        }

        private RecordedPanel mRecordedPanel;
    }

    private static final ColumnData mColumns[] = { new ColumnData(" ", 5, JLabel.CENTER),
            new ColumnData("Title", 160, JLabel.LEFT), new ColumnData("Episode", 140, JLabel.LEFT),
            new ColumnData("Date Recorded", 70, JLabel.RIGHT), new ColumnData("Duration", 30, JLabel.RIGHT),
            new ColumnData("Size", 30, JLabel.RIGHT), new ColumnData("Status", 100, JLabel.RIGHT) };

    public RecordedPanel() {
        super();

        setLayout(new BorderLayout());

        mRecorded = new ArrayList();

        final ShowTableData showsTableData = new ShowTableData();
        //final ShowTableData showsTableData = new ShowTableData(new ArrayList());
        mTable = new JTable();
        mTable.setDefaultRenderer(Object.class, new CustomTableCellRender());

        mTitleField = new JLabel(" ", JLabel.LEADING);
        Font font = mTitleField.getFont().deriveFont(Font.BOLD, mTitleField.getFont().getSize());
        mTitleField.setFont(font);
        mDescriptionField = new JLabel(" ", JLabel.LEADING);
        mDateField = new JLabel(" ", JLabel.LEADING);
        mChannelStationField = new JLabel(" ", JLabel.TRAILING);
        mRatingField = new JLabel(" ", JLabel.LEADING);
        mQualityField = new JLabel(" ", JLabel.TRAILING);

        FormLayout layout = new FormLayout("left:pref, 3dlu, right:pref:grow", "pref, " + //title
                "3dlu, " + "pref, " + //description
                "3dlu, " + "pref, " + //date/channel
                "3dlu, " + "pref " //rating/quality
        );

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.add(mTitleField, cc.xyw(1, 1, 3));
        builder.add(mDescriptionField, cc.xyw(1, 3, 3));
        builder.add(mDateField, cc.xy(1, 5));
        builder.add(mChannelStationField, cc.xy(3, 5));
        builder.add(mRatingField, cc.xy(1, 7));
        builder.add(mQualityField, cc.xy(3, 7));

        add(builder.getPanel(), BorderLayout.NORTH);

        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());

        mTable.setModel(showsTableData);
        TableColumn column = null;
        for (int i = 0; i < 7; i++) {
            column = mTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(mColumns[i].mWidth);
        }
        mTable.setDragEnabled(true);
        mTable.setRowSelectionAllowed(true);
        mTable.setCellSelectionEnabled(false);
        //mTable.setColumnSelectionAllowed(false);
        ListSelectionModel selectionModel = mTable.getSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                //Ignore extra messages.
                if (e.getValueIsAdjusting())
                {
                    return;
                }
                
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (!lsm.isSelectionEmpty()) {
                    if (!mUpdating) {
                        int selectedRow = lsm.getMinSelectionIndex();
                        mTitleField.setText((String) mTable.getModel().getValueAt(selectedRow, 1));
                        mDescriptionField.setText((String) mTable.getModel().getValueAt(selectedRow, 7));
                        mDateField.setText((String) mTable.getModel().getValueAt(selectedRow, 3));
                        mChannelStationField.setText((String) mTable.getModel().getValueAt(selectedRow, 8));
                        mRatingField.setText((String) mTable.getModel().getValueAt(selectedRow, 9));
                        mQualityField.setText((String) mTable.getModel().getValueAt(selectedRow, 10));
                    }
                } else {
                    if (!mUpdating) {
                        mTitleField.setText(" ");
                        mDescriptionField.setText(" ");
                        mDateField.setText(" ");
                        mChannelStationField.setText(" ");
                        mRatingField.setText(" ");
                        mQualityField.setText(" ");
                    }
                }
            }
        });
        if (mTable.getModel().getRowCount() > 0)
            mTable.setRowSelectionInterval(0, 0);

        JTableHeader header = mTable.getTableHeader();
        header.setUpdateTableInRealTime(true);
        header.addMouseListener(showsTableData.new ColumnListener(mTable));
        header.setReorderingAllowed(true);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(mTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        add(tablePanel, BorderLayout.CENTER);

        mRecordedThread = new RecordedThread(this);
        mRecordedThread.setPriority(Thread.MIN_PRIORITY);
    }

    class ShowTableData extends AbstractTableModel {

        protected int mSortCol = 3;

        protected boolean mSortAsc = true;

        protected SimpleDateFormat mDateFormat;

        protected SimpleDateFormat mTimeFormat;

        protected GregorianCalendar mCalendar;

        protected DecimalFormat mNumberFormat;

        public ShowTableData() {
            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d hh:mm");
            mTimeFormat = new SimpleDateFormat();
            mTimeFormat.applyPattern("H:mm");
            mCalendar = new GregorianCalendar();
            mNumberFormat = new DecimalFormat("###,###");
        }

        public int getRowCount() {
            return mRecorded == null ? 0 : mRecorded.size();
        }

        public int getColumnCount() {
            return 7;
        }

        public String getColumnName(int column) {
            String str = mColumns[column].mTitle;
            if (column == mSortCol)
                str += mSortAsc ? " »" : " «";
            return str;
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int nRow, int nCol) {
            if (nCol == 0)
            {
                Show show = (Show) mRecorded.get(nRow);
                return show.getStatus()!=Show.STATUS_RECORDING;
            }
            else
                return false;
        }

        public Object getValueAt(int nRow, int nCol) {
            if (nRow < 0 || nRow >= getRowCount())
                return "";
            Show show = (Show) mRecorded.get(nRow);
            switch (nCol) {
            case 0:
                return new Boolean(show.getStatus()==Show.STATUS_PENDING || show.getStatus()==Show.STATUS_USER_SELECTED || show.getStatus()==Show.STATUS_DOWNLOADING);
            case 1:
                return show.getTitle();
            case 2:
                return show.getEpisode();
            case 3:
                // Round off to the closest minutes; TiVo seems to start recordings 2 seconds before the scheduled time
                mCalendar.setTime(show.getDateRecorded());
                mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                        + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
                mCalendar.set(GregorianCalendar.SECOND, 0);
                return mDateFormat.format(mCalendar.getTime());
            case 4:
                // Round off to closest minute
                //int duration = Math.round((show.getDuration()/1000/60+0.5f)/10)*10;
                int duration = Math.round(show.getDuration() / 1000 / 60 + 0.5f);
                mCalendar.setTime(new Date(Math.round((show.getDuration() / 1000 / 60 + 0.5f) / 10) * 10));
                mCalendar.set(GregorianCalendar.HOUR_OF_DAY, duration / 60);
                mCalendar.set(GregorianCalendar.MINUTE, duration % 60);
                mCalendar.set(GregorianCalendar.SECOND, 0);
                return mTimeFormat.format(mCalendar.getTime());
            case 5:
                return mNumberFormat.format(show.getSize() / (1024 * 1024)) + " MB";
            case 6:
                return show.getStatusString();
            case 7:
                return show.getDescription().length() != 0 ? show.getDescription() : " ";
            case 8:
                return show.getChannel() + " " + show.getStation();
            case 9:
                return show.getRating().length() != 0 ? show.getRating() : "No rating";
            case 10:
                return show.getQuality();
            }
            return " ";
        }

        public void setValueAt(Object value, final int row, int col) {
            Show show = null;
            if (row < mRecorded.size())
                show = (Show) mRecorded.get(row);

            if (show == null) {
                show = new Show();
                mRecorded.add(row, show);
            }

            if (col == 0) {
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        mTable.setRowSelectionInterval(0, row);
                    }
                });
                
                Boolean status = (Boolean)value;
                if (status.booleanValue())
                    show.setStatus(Show.STATUS_USER_SELECTED);
                else    
                    show.setStatus(Show.STATUS_USER_CANCELLED);
                
                boolean found = false;
                ToGoList togoList = new ToGoList();
                ArrayList downloads = togoList.load();
                Iterator recordedIterator = downloads.iterator();
                while (recordedIterator.hasNext())
                {
                    Show downloaded = (Show)recordedIterator.next();
                    
                    if (downloaded.equals(show))
                    {
                        downloaded.setStatus(show.getStatus());
                        found = true;
                        break;
                    }
                }
                if (!found)
                    downloads.add(show);
                togoList.save(downloads);
            }
            fireTableDataChanged();
        }

        public void removeRow(int row) {
            mRecorded.remove(row);
            fireTableDataChanged();
        }

        class ColumnListener extends MouseAdapter {
            protected JTable mTable;

            public ColumnListener(JTable table) {
                mTable = table;
            }

            public void mouseClicked(MouseEvent e) {
                TableColumnModel colModel = mTable.getColumnModel();
                int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
                int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

                if (modelIndex < 0)
                    return;
                if (mSortCol == modelIndex)
                    mSortAsc = !mSortAsc;
                else
                    mSortCol = modelIndex;

                for (int i = 0; i < mColumns.length; i++) {
                    TableColumn column = colModel.getColumn(i);
                    column.setHeaderValue(getColumnName(column.getModelIndex()));
                    column.setPreferredWidth(mColumns[i].mWidth);
                }
                mTable.getTableHeader().repaint();

                Collections.sort(mRecorded, new ShowComparator(modelIndex, mSortAsc));
                mTable.tableChanged(new TableModelEvent(ShowTableData.this));
                mTable.repaint();
                if (mTable.getModel().getRowCount() > 0)
                    mTable.setRowSelectionInterval(0, 0);
            }
        }

    }
    
    class ShowComparator implements Comparator {
        protected int mSortCol;

        protected boolean mSortAsc;

        public ShowComparator(int sortCol, boolean sortAsc) {
            mSortCol = sortCol;
            mSortAsc = sortAsc;
        }

        public int compare(Object o1, Object o2) {
            Show contact1 = (Show) o1;
            Show contact2 = (Show) o2;
            int result = 0;
            double d1, d2;
            switch (mSortCol) {
            case 0:
                result = contact1.getTitle().compareTo(contact2.getTitle());
                break;
            case 1:
                result = contact1.getTitle().compareTo(contact2.getTitle());
                break;
            case 2:
                result = contact1.getEpisode().compareTo(contact2.getEpisode());
                break;
            case 3:
                result = contact1.getDateRecorded().compareTo(contact2.getDateRecorded());
                break;
            case 4:
                Integer duration1 = new Integer(contact1.getDuration());
                Integer duration2 = new Integer(contact2.getDuration());
                result = duration1.compareTo(duration2);
                break;
            case 5:
                Long size1 = new Long(contact1.getSize());
                Long size2 = new Long(contact2.getSize());
                result = size1.compareTo(size2);
                break;
            case 6:
                result = contact1.getStatusString().compareTo(contact2.getStatusString());
                break;
            }

            if (!mSortAsc)
                result = -result;
            return result;
        }
    }

    class CustomTableCellRender extends DefaultTableCellRenderer {
        public CustomTableCellRender() {
            super();
            URL url = getClass().getClassLoader().getResource("guiicon.gif");
            mYellowIcon = new ImageIcon(getClass().getClassLoader().getResource("yellowball.gif"));
            mYellowExclamationIcon = new ImageIcon(getClass().getClassLoader().getResource("yellowball!.gif"));
            mWhiteIcon = new ImageIcon(getClass().getClassLoader().getResource("whiteball.gif"));
            mGreenIcon = new ImageIcon(getClass().getClassLoader().getResource("greenball.gif"));
            mRedIcon = new ImageIcon(getClass().getClassLoader().getResource("redball.gif"));
            mBlueIcon = new ImageIcon(getClass().getClassLoader().getResource("blueball.gif"));
            mEmptyIcon = new ImageIcon(getClass().getClassLoader().getResource("empty.gif"));
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, mTable.getSelectedRow()==row, hasFocus, row, column);

            if (mFont == null) {
                mFont = table.getFont().deriveFont(Font.BOLD, table.getFont().getSize());
            }

            setHorizontalAlignment(mColumns[column].mAlignment);
            //setFont(mFont);
            if (column == 1) {
                Show show = (Show) mRecorded.get(row);
                if (show.getIcon().equals("in-progress-recording"))
                    setIcon(mRedIcon);
                else if (show.getIcon().equals("expires-soon-recording"))
                    setIcon(mYellowIcon);
                else if (show.getIcon().equals("expired-recording"))
                    setIcon(mYellowExclamationIcon);
                else
                    setIcon(mEmptyIcon);

            } else
                setIcon(null);
            
            /*
            if (isSelected)
                setBackground(Color.BLUE);
            else
                setBackground(Color.WHITE);
                */

            return this;
        }

        private Font mFont;

        private ImageIcon mYellowIcon;

        private ImageIcon mYellowExclamationIcon;

        private ImageIcon mWhiteIcon;

        private ImageIcon mGreenIcon;

        private ImageIcon mRedIcon;

        private ImageIcon mBlueIcon;
        
        private ImageIcon mEmptyIcon;
    }

    public void activate() {
        if (Galleon.getServerConfiguration().getRecordingsPath().length() == 0) {
            JOptionPane
                    .showMessageDialog(
                            this,
                            "You have not configured the ToGo recordings path using the File/Properties menu.",
                            "Error", JOptionPane.ERROR_MESSAGE);
        }
        else
        if (Tools.decrypt(Galleon.getServerConfiguration().getMediaAccessKey()).length() == 0) {
            JOptionPane
                    .showMessageDialog(
                            this,
                            "You have not configured the ToGo media access key using the File/Properties menu.",
                            "Error", JOptionPane.ERROR_MESSAGE);
        }        
        else
        if (mRecorded.size() == 0 && !mRecordedThread.isAlive()) {
            RecordedPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            mTitleField.setText("Retrieving recording data from your TiVo(s)...");
            mRecordedThread.start();
        }
    }
    
    public void updateTable()
    {
        Galleon.getToGo().applyRules();
        
        ToGoList togoList = new ToGoList();
        mRecorded = togoList.load(Show.STATUS_DOWNLOADING | Show.STATUS_PENDING | Show.STATUS_USER_CANCELLED | Show.STATUS_RECORDED | Show.STATUS_RECORDING | Show.STATUS_INCOMPLETE | Show.STATUS_USER_SELECTED);
        
        Collections.sort(mRecorded, new ShowComparator(3, false));
        ShowTableData model = (ShowTableData)mTable.getModel(); 
        model.fireTableDataChanged();
        
        if (model.getRowCount() > 0)
            mTable.setRowSelectionInterval(0, 0);
    }
    
    public void progress(String value)
    {
        mDescriptionField.setText(value);
        ShowTableData model = (ShowTableData) mTable.getModel();
        model.fireTableDataChanged();
    }

    private JTable mTable;

    private JLabel mTitleField;

    private JLabel mDescriptionField;

    private JLabel mDateField;

    private JLabel mChannelStationField;

    private JLabel mRatingField;

    private JLabel mQualityField;

    private boolean mUpdating;

    private ArrayList mRecorded;

    private RecordedThread mRecordedThread;
}