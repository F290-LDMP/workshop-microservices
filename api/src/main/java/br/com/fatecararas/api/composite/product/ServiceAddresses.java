package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Instâncias que atenderam à consulta")
public class ServiceAddresses {
    private String cmp;
    private String pro;
    private String rev;
    private String rec;

    public ServiceAddresses() { }

    public ServiceAddresses(String cmp, String pro, String rev, String rec) {
        this.cmp = cmp;
        this.pro = pro;
        this.rev = rev;
        this.rec = rec;
    }

    public String getCmp() { return cmp; }
    public void setCmp(String cmp) { this.cmp = cmp; }
    public String getPro() { return pro; }
    public void setPro(String pro) { this.pro = pro; }
    public String getRev() { return rev; }
    public void setRev(String rev) { this.rev = rev; }
    public String getRec() { return rec; }
    public void setRec(String rec) { this.rec = rec; }
}
