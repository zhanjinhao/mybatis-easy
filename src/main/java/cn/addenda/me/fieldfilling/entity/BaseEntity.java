package cn.addenda.me.fieldfilling.entity;

import cn.addenda.businesseasy.json.LocalDateTimeStrDeSerializer;
import cn.addenda.businesseasy.json.LocalDateTimeStrSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;

/**
 * @author addenda
 * @datetime 2022/8/16 20:40
 */
@JsonIgnoreProperties({
        BaseEntity.N_CREATOR, BaseEntity.N_CREATOR_NAME, BaseEntity.N_CREATE_TIME, BaseEntity.N_MODIFIER,
        BaseEntity.N_MODIFIER_NAME, BaseEntity.N_MODIFY_TIME, BaseEntity.N_REMARK})
public abstract class BaseEntity {

    public static final String N_CREATOR = "creator";
    public static final String N_CREATOR_NAME = "creatorName";
    public static final String N_CREATE_TIME = "createTime";
    public static final String N_MODIFIER = "modifier";
    public static final String N_MODIFIER_NAME = "modifierName";
    public static final String N_MODIFY_TIME = "modifyTime";
    public static final String N_REMARK = "remark";

    private String creator;
    private String creatorName;

    @JsonSerialize(using = LocalDateTimeStrSerializer.class)
    @JsonDeserialize(using = LocalDateTimeStrDeSerializer.class)
    private LocalDateTime createTime;

    private String modifier;
    private String modifierName;

    @JsonSerialize(using = LocalDateTimeStrSerializer.class)
    @JsonDeserialize(using = LocalDateTimeStrDeSerializer.class)
    private LocalDateTime modifyTime;

    private String remark;

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public String getModifierName() {
        return modifierName;
    }

    public void setModifierName(String modifierName) {
        this.modifierName = modifierName;
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

    @Override
    public String toString() {
        return "BaseEntity{" +
                "creator='" + creator + '\'' +
                ", creatorName='" + creatorName + '\'' +
                ", createTime=" + createTime +
                ", modifier='" + modifier + '\'' +
                ", modifierName='" + modifierName + '\'' +
                ", modifyTime=" + modifyTime +
                ", remark='" + remark + '\'' +
                '}';
    }
}
