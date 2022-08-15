package cn.addenda.me.fieldfilling.entity;

import java.time.LocalDateTime;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 14:16
 */
public abstract class BaseEntity {

    private String createUser;

    private LocalDateTime createTime;

    private String modifyUser;

    private LocalDateTime modifyTime;

    private String remark;

    private byte delFg;

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getModifyUser() {
        return modifyUser;
    }

    public void setModifyUser(String modifyUser) {
        this.modifyUser = modifyUser;
    }

    public LocalDateTime getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(LocalDateTime modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public byte getDelFg() {
        return delFg;
    }

    public void setDelFg(byte delFg) {
        this.delFg = delFg;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "createUser='" + createUser + '\'' +
                ", createTime=" + createTime +
                ", modifyUser='" + modifyUser + '\'' +
                ", modifyTime=" + modifyTime +
                ", remark='" + remark + '\'' +
                ", delFg=" + delFg +
                '}';
    }
}
