package com.captures.converter.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="conversions")
public class Conversion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) private UserAccount owner;
 @Column(length=128) private String guestKey;
 @Lob @Column(nullable=false) private byte[] sourceData;
 @Lob @Column(nullable=false) private byte[] resultData;
 @Column(nullable=false) private String sourceName, sourceFormat, resultFormat, contentType;
 @Column(nullable=false) private long sourceSize, resultSize;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 public Conversion(){}
 public Conversion(UserAccount owner,String guestKey,String sourceName,String sourceFormat,String resultFormat,String contentType,byte[] sourceData,byte[] resultData){
 this.guestKey=guestKey;
 this.owner=owner;this.sourceName=sourceName;this.sourceFormat=sourceFormat;this.resultFormat=resultFormat;this.contentType=contentType;
 this.sourceData=sourceData;this.resultData=resultData;this.sourceSize=sourceData.length;this.resultSize=resultData.length;}
 public Long getId(){return id;} public UserAccount getOwner(){return owner;} public byte[] getSourceData(){return sourceData;} public byte[] getResultData(){return resultData;}
 public String getGuestKey(){return guestKey;}
 public String getSourceName(){return sourceName;} public String getSourceFormat(){return sourceFormat;} public String getResultFormat(){return resultFormat;}
 public String getContentType(){return contentType;} public long getSourceSize(){return sourceSize;} public long getResultSize(){return resultSize;} public Instant getCreatedAt(){return createdAt;}
}
