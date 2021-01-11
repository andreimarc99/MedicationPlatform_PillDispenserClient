package client.entities;

import java.util.Date;

public class RPCObject {
    private String med_name;
    private Long prescription_id;
    private Long patient_id;
    private Date startTime;
    private Date endTime;

    public RPCObject(String med_name, Long prescription_id, Long patient_id, Date startTime, Date endTime) {
        this.med_name = med_name;
        this.prescription_id = prescription_id;
        this.patient_id = patient_id;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getMed_name() {
        return med_name;
    }

    public void setMed_name(String med_name) {
        this.med_name = med_name;
    }

    public Long getPrescription_id() {
        return prescription_id;
    }

    public void setPrescription_id(Long prescription_id) {
        this.prescription_id = prescription_id;
    }

    public Long getPatient_id() {
        return patient_id;
    }

    public void setPatient_id(Long patient_id) {
        this.patient_id = patient_id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String toString() {
        return "Prescription " + prescription_id + ": medicine: " + med_name + ", patient: " + patient_id + "; startTime: " + startTime + ", endTime: " + endTime;
    }
}
