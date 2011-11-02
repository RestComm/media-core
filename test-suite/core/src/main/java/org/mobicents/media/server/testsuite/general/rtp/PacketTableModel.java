/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.testsuite.general.rtp;

import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import org.mobicents.media.server.testsuite.general.rtp.RtpPacket;

/**
 *
 * @author kulikov
 */
public class PacketTableModel extends DefaultTableModel {

    private SimpleDateFormat fmt = new SimpleDateFormat("mm:ss,SSS");
    
    private String[] columnNames = new String[]{
        "Time", "Seq number", "SSRC", "Timestamp", "Payload"
    };

    private List<RtpPacket> packets;
    
    public PacketTableModel(List packets) {
        this.packets = packets;
    }
    
    @Override
    public Class getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        int count = packets != null ? packets.size() : 0;
        return count;
    }

    private String getFormat(int pt) {
        if (pt == 8) {
            return "PCMA";
        } else if (pt == 0) {
            return "PCMU";
        } else return "UNKNOWN";
    }
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RtpPacket packet = packets.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return fmt.format(packet.getTime());
            case 1:
                return packet.getSeqNumber();
            case 2:
                return packet.getSSRC();
            case 3:                
                return packet.getTimestamp();
            case 4:
                return getFormat(packet.getPayloadType());
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    }
    

}
