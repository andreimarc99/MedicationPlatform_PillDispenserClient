package client;

import client.entities.RPCObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Window extends JFrame {

    private static JTable table;
    private static DefaultTableModel tableModel;

    private Connection connection;
    private Channel channel;
    private String Q_NAME = "assig3_queue";

    public Window() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Prescriptions");
        setBounds(0, 550, 900, 500);
        getContentPane().setLayout(null);

        JLabel timeLabel = new JLabel();

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String time = LocalTime.now().format(formatter);
                timeLabel.setText(time);
            }
        }, 0, 1000);
        getContentPane().add(timeLabel);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    reportNotTakenMedicine();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000);

        timeLabel.setBounds(420, 30, 100, 20);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(25, 70, 828, 338);
        getContentPane().add(scrollPane);

        table = new JTable();
        table.setModel(
                new DefaultTableModel(new Object[][] {}, new String[] {"Med name", "Prescription ID", "Patient ID", "Lower limit take time", "Upper limit take time" }) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        //all cells false
                        return false;
                    }
                });
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        tableModel = (DefaultTableModel) table.getModel();

        scrollPane.setViewportView(table);

        JButton takeButton = new JButton("Take");
        takeButton.setBounds(750, 420, 100, 25);
        takeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(tableModel.getDataVector().elementAt(table.getSelectedRow()));
                try {
                    takeMedicine();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException | ParseException ex) {
                    ex.printStackTrace();
                }
            }
        });
        getContentPane().add(takeButton);

        JButton downloadButton = new JButton("Download");
        downloadButton.setBounds(600, 420, 100, 25);
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    downloadPrescriptions();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (ParseException ex) {
                    ex.printStackTrace();
                }
            }
        });
        getContentPane().add(downloadButton);
    }

    public String sendRequest(String message) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().correlationId(corrId).replyTo(replyQueueName).build();

        channel.basicPublish("", Q_NAME, properties, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String cTag = channel.basicConsume(replyQueueName, true, (cons, deliv) -> {
            if (deliv.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(deliv.getBody(), "UTF-8"));
            }
        }, cons -> { });

        String result = response.take();
        channel.basicCancel(cTag);
        return result;
    }

    private void downloadPrescriptions() throws IOException, InterruptedException, ParseException {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0);

        java.lang.reflect.Type receivedObj = new TypeToken<ArrayList<RPCObject>>(){}.getType();
        String s = sendRequest("download");
        Gson gson = new Gson();
        ArrayList<RPCObject> objs = gson.fromJson(s, receivedObj);

        Calendar c = Calendar.getInstance();
        for (RPCObject o : objs) {
            String sDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.UK).parse(o.getStartTime().toString()));
            String eDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.UK).parse(o.getEndTime().toString()));
            Date startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sDate);
            Date endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(eDate);
            //System.out.println(o.toString());
            System.out.println(startDate + " --- " + endDate);
            System.out.println(c.getTime() + "--- now");
            if (startDate.compareTo(c.getTime()) < 0 || (startDate.getYear() == c.getTime().getYear() && startDate.getMonth() == c.getTime().getMonth()
            && startDate.getDate() == c.getTime().getDate())) {
                if (endDate.compareTo(c.getTime()) > 0 || (endDate.getYear() == c.getTime().getYear() && endDate.getMonth() == c.getTime().getMonth()
                        && endDate.getDate() == c.getTime().getDate())) {
                    if ((endDate.getHours() > c.getTime().getHours()) ||
                            (endDate.getHours() == c.getTime().getHours() && endDate.getMinutes() > c.getTime().getMinutes()) ||
                            (endDate.getHours() == c.getTime().getHours() && endDate.getMinutes() == c.getTime().getMinutes() && endDate.getSeconds() > c.getTime().getSeconds())) {
                        tableModel.addRow(new Object[]{o.getMed_name(), o.getPrescription_id(), o.getPatient_id(), o.getStartTime(), o.getEndTime()});
                    }
                }
            }
        }
    }

    private void takeMedicine() throws IOException, InterruptedException, ParseException {
        Calendar c = Calendar.getInstance();
        if (!tableModel.getDataVector().elementAt(table.getSelectedRow()).equals(null)) {
            Date sDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.UK).parse(tableModel.getValueAt(table.getSelectedRow(), 3).toString());
            Date eDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.UK).parse(tableModel.getValueAt(table.getSelectedRow(), 4).toString());

            System.out.println(sDate);
            if (sDate.compareTo(c.getTime()) < 0 && eDate.compareTo(c.getTime()) > 0) {
                String s = sendRequest("taken," + tableModel.getValueAt(table.getSelectedRow(), 0) + "," +
                        tableModel.getValueAt(table.getSelectedRow(), 1) + "," + tableModel.getValueAt(table.getSelectedRow(), 2) +
                        "," + tableModel.getValueAt(table.getSelectedRow(), 3) + "," + tableModel.getValueAt(table.getSelectedRow(), 4));
                System.out.println(s);
                JOptionPane.showMessageDialog(this, "You have successfully taken " + tableModel.getValueAt(table.getSelectedRow(), 0) + ".");
                tableModel.removeRow(table.getSelectedRow());
            }
        }
    }

    private void reportNotTakenMedicine() throws IOException, InterruptedException {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
        String time = LocalTime.now().format(formatter);
        Date now = new Date();
        try {
            now = new SimpleDateFormat("HH:mm:ss").parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        JTable tempTable = table;
        DefaultTableModel tempModel = (DefaultTableModel) tempTable.getModel();
        for (int i = 0; i < tempTable.getRowCount(); ++i) {
            Date eDate = new Date();
            try {
                eDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.UK).parse(tempModel.getValueAt(i, 4).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if ((eDate.getHours() < now.getHours()) ||
                    (eDate.getHours() == now.getHours() && eDate.getMinutes() < now.getMinutes()) ||
                    (eDate.getHours() == now.getHours() && eDate.getMinutes() == now.getMinutes() && eDate.getSeconds() < now.getSeconds())) {
                        JOptionPane.showMessageDialog(this, "You failed to take " + tempModel.getValueAt(i, 0) + " before " +
                                tempModel.getValueAt(i, 4));

                String s = sendRequest("not_taken," + tempModel.getValueAt(i, 0) + "," + tempModel.getValueAt(i, 1) + "," +
                        tempModel.getValueAt(i, 2));
                System.out.println(s);

                tempModel.removeRow(i);
            }
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri("amqps://rsvouwnv:AUQf84tBGLs3z5idn8YEkOz2Y26wUaJp@shark.rmq.cloudamqp.com/rsvouwnv");

        Window window = new Window();
        window.setVisible(true);

        try {
            window.connection = factory.newConnection();
            window.channel = window.connection.createChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
