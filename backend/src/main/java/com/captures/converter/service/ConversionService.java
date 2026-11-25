package com.captures.converter.service;
import com.captures.converter.dto.Dtos.*;
import com.captures.converter.entity.*;
import com.captures.converter.exception.ApiException;
import com.captures.converter.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.*;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import java.util.*;

@Service
public class ConversionService {
 private final ConversionRepository conversions;
 private final GuestUsageRepository guestUsage;
 private final UserRepository users;
 private final ServerMetricsService serverMetrics;
 public ConversionService(ConversionRepository conversions,GuestUsageRepository guestUsage,UserRepository users,ServerMetricsService serverMetrics){this.conversions=conversions;this.guestUsage=guestUsage;this.users=users;this.serverMetrics=serverMetrics;}

 public ConversionResponse convert(MultipartFile file,String format,Integer quality,Integer width,Integer height,String guestKey,String email){
  if(file.isEmpty())throw new ApiException(HttpStatus.BAD_REQUEST,"Файл не выбран");
  UserAccount owner=email==null?null:users.findByEmail(email).orElse(null);
  long perFileLimit=owner!=null?500L*1024*1024:50L*1024*1024;
  if(file.getSize()>perFileLimit)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Один файл превышает лимит "+(owner!=null?500:50)+" MB");
  if(quality!=null&&(quality<1||quality>100))throw new ApiException(HttpStatus.BAD_REQUEST,"Качество должно быть от 1 до 100");
  if(width!=null&&(width<1||width>10000))throw new ApiException(HttpStatus.BAD_REQUEST,"Ширина должна быть от 1 до 10000 px");
  if(height!=null&&(height<1||height>10000))throw new ApiException(HttpStatus.BAD_REQUEST,"Высота должна быть от 1 до 10000 px");
  if(owner==null){
   GuestUsage u=guestUsage.findByGuestKeyAndUsageDate(guestKey,LocalDate.now()).orElse(new GuestUsage(guestKey,LocalDate.now()));
   if(u.getCount()>=10)throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Гостевой лимит: 10 обработок");
  }else if(owner.getRole()!=Role.ADMIN){
   Instant from=Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
   if(conversions.countByOwnerAfter(owner,from)>=150)throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Дневной лимит: 150 изображений");
   if(conversions.sumSourceByOwnerAfter(owner,from)+file.getSize()>500L*1024*1024)throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Дневной лимит размера: 500 MB");
  }
  String target=(format==null?"PNG":format).toUpperCase(Locale.ROOT).replace("JPEG","JPG");
  if(!Set.of("PNG","JPG","WEBP","GIF","TIFF","HEIC","AVIF").contains(target))throw new ApiException(HttpStatus.BAD_REQUEST,"Неподдерживаемый формат");
  try{
   BufferedImage source;
   try(InputStream in=file.getInputStream(); var readers=ImageIO.createImageInputStream(in)){
    if(readers==null)throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Не удалось определить формат изображения");
    Iterator<ImageReader> it=ImageIO.getImageReaders(readers);
    if(!it.hasNext())throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Формат не поддерживается установленными ImageIO-плагинами");
    ImageReader reader=it.next();reader.setInput(readers,true,true);
    source=reader.read(0);reader.dispose();
   }
   if(source==null)throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Изображение не удалось декодировать");
   if(width!=null||height!=null){int w=width!=null?width:Math.round(source.getWidth()*height/(float)source.getHeight());int h=height!=null?height:Math.round(source.getHeight()*width/(float)source.getWidth());source=resize(source,w,h);}
   byte[] result=encode(source,target,quality==null?82:quality);
   String sourceFormat=guessFormat(file.getOriginalFilename(),file.getContentType());
   Conversion c=conversions.save(new Conversion(owner,owner==null?guestKey:null,file.getOriginalFilename(),sourceFormat,target,target.equals("JPG")?"image/jpeg":"image/"+target.toLowerCase(),file.getBytes(),result));
   if(owner==null){
    GuestUsage u=guestUsage.findByGuestKeyAndUsageDate(guestKey,LocalDate.now()).orElse(new GuestUsage(guestKey,LocalDate.now()));
    u.increment();
    guestUsage.save(u);
   }
   return new ConversionResponse(c.getId(),c.getSourceName(),c.getSourceFormat(),c.getResultFormat(),c.getSourceSize(),c.getResultSize(),java.util.Date.from(c.getCreatedAt()),"/api/conversions/"+c.getId()+"/download");
  }catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"Не удалось обработать изображение: "+e.getMessage());}
 }

 private BufferedImage resize(BufferedImage source,int w,int h){
  BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,0,0,w,h,null);g.dispose();return out;
 }
 private byte[] encode(BufferedImage image,String target,int quality) throws Exception{
  if(Set.of("HEIC","AVIF").contains(target)){
   throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,target+" декодирование/кодирование недоступно: на сервере нет совместимого нативного плагина");
  }
  String writerFormat=target.equals("JPG")?"jpeg":target.toLowerCase();
  Iterator<ImageWriter> writers=ImageIO.getImageWritersByFormatName(writerFormat);
  if(!writers.hasNext())throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Выходной формат "+target+" не поддерживается установленными ImageIO-плагинами");
  ImageWriter writer=writers.next();ByteArrayOutputStream out=new ByteArrayOutputStream();
  try(ImageOutputStream ios=ImageIO.createImageOutputStream(out)){writer.setOutput(ios);ImageWriteParam p=writer.getDefaultWriteParam();
   if(p.canWriteCompressed()){
    p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    if(p instanceof JPEGImageWriteParam){p.setCompressionQuality(Math.max(.01f,Math.min(1f,quality/100f)));}else{p.setCompressionQuality(Math.max(.01f,Math.min(1f,quality/100f)));}}
   BufferedImage output=image;if(target.equals("JPG")&&(image.getType()==BufferedImage.TYPE_CUSTOM||image.getColorModel().hasAlpha()))output=flatten(image);
   writer.write(null,new IIOImage(output,null,null),p);
  }finally{writer.dispose();}return out.toByteArray();
 }
 private BufferedImage flatten(BufferedImage source){BufferedImage out=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,out.getWidth(),out.getHeight());g.drawImage(source,0,0,null);g.dispose();return out;}
 private String guessFormat(String name,String type){String n=name==null?"":name.toLowerCase(Locale.ROOT);String ext=n.contains(".")?n.substring(n.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";if(!ext.isEmpty())return ext;return type==null?"UNKNOWN":type.replace("image/","").replace("jpeg","jpg").toUpperCase(Locale.ROOT);}
 public Conversion getConversion(Long id){return conversions.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Конвертация не найдена"));}
 public Page<ConversionResponse> history(UserAccount owner,int page,int size){return conversions.findByOwnerOrderByCreatedAtDesc(owner,PageRequest.of(page,size)).map(this::response);}
 private ConversionResponse response(Conversion c){return new ConversionResponse(c.getId(),c.getSourceName(),c.getSourceFormat(),c.getResultFormat(),c.getSourceSize(),c.getResultSize(),java.util.Date.from(c.getCreatedAt()),"/api/conversions/"+c.getId()+"/download");}
 public QuotaResponse quota(UserAccount owner,GuestUsage guest){if(owner!=null){if(owner.getRole()==Role.ADMIN)return new QuotaResponse(true,"ADMIN",-1,0,-1,0);Instant from=Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();return new QuotaResponse(true,"CLIENT",150,conversions.countByOwnerAfter(owner,from),500L*1024*1024,conversions.sumSourceByOwnerAfter(owner,from));}return new QuotaResponse(false,"GUEST",10,guest==null?0:guest.getCount(),50L*1024*1024,0);}
 public StatisticsResponse stats(){return new StatisticsResponse(conversions.countAllConversions(),conversions.sumAllSourceBytes(),conversions.sumAllResultBytes(),serverMetrics.read());}
}
