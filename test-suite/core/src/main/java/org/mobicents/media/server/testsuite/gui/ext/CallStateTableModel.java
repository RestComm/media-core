/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.testsuite.gui.ext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableModel;
import org.mobicents.media.server.testsuite.general.AbstractCall;

/**
 *
 * @author baranowb
 */
public class CallStateTableModel 
extends  DefaultTableModel
   {
   
       private final static String[] _COLUMN_NAMES = new String[]{"Seq","Endpoint","CallID","Avg Jitter","Peak Jitter","State"};
       private final static Class[] _COLUMN_TYPE = new Class[]{Long.class,String.class,Object.class,Long.class,Long.class,Object.class};
       private Map<Long, AbstractCall> callSequenceToCall = null;

    public CallStateTableModel(Map<Long, AbstractCall> _callSequenceToCall) {
        this.callSequenceToCall=_callSequenceToCall;
    }

        public void setCallData(Map<Long, AbstractCall> _callSequenceToCall) {
            this.callSequenceToCall=_callSequenceToCall;
        }
       public Map<Long, AbstractCall> getCallData()
       {
           return this.callSequenceToCall;
       }
        public int getRowCount() {
            int count = this.callSequenceToCall == null ? 0 :this.callSequenceToCall.size();
            //System.err.println("-- GET RC: "+count+" - "+(this.callSequenceToCall == null));
            return count;
        }

        public int getColumnCount() {
           return _COLUMN_NAMES.length;
        }

        public String getColumnName(int columnIndex) {
           return _COLUMN_NAMES[columnIndex];
        }

        public Class<?> getColumnClass(int columnIndex) {
            return _COLUMN_TYPE[columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            
            //System.err.println("GET V["+rowIndex+"]["+rowIndex+"] --- "+this.callSequenceToCall.keySet());
            AbstractCall call = this.callSequenceToCall.get(new Long(rowIndex));
            
            if(call  == null)
            {
                return "NO VALUE";
            }
            switch(columnIndex)
            {
                case 0:
                        return call.getSequence();
    
                case 1:
                        return call.getEndpoint();
                    
                case 2:
                        return call.getCallID();
                    
                case 3:
                        return call.getAvgJitter();
                    
                case 4:
                        return call.getPeakJitter();
                    
                case 5:
                        return call.getState();
                    
                default :
                    return null;
                
            }
        }   
       
}
