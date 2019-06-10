package com.pointlion.sys.mvc.admin.sys.generator;

import java.io.*;

import com.jfinal.kit.Kv;
import com.jfinal.kit.PathKit;
import com.jfinal.template.Engine;
import com.jfinal.template.Template;

/***
 * jfinal魔板引擎
 * @author dufuzhong
 */
public class Enjoy {
	
	static final Integer FAIL = -1;
	static final Integer SUCCESS = 0;
	static final Integer EXIST = 1;

    /**
     * 根据具体模板生成文件
     * @param templateFileName  模板文件名称
     * @param kv                渲染参数
     * @param filePath          输出目录
     * @return 
     * 1：已存在
     * 0：成功
     * -1：失败
     */
    public Integer render(String templateFileName, Kv kv, String filePath)  {
        BufferedWriter output = null;
//        OutputStreamWriter output = null;
        try {
//            String baseTemplatePath = new StringBuilder(PathKit.getRootClassPath())
//            .append("\\")
//            .append(PathKit.getPackagePath(this))
//            .append("\\template")
//            .toString();
            // TODO 路径斜杠替换
//            String replacePath = baseTemplatePath.replace("\\", "/");
            // 写死 路径
            String baseTemplatePath = "C:\\Users\\xinchuang\\Desktop\\oa\\JFinalOA-V2\\JFinalOA\\src\\main\\java\\com\\pointlion\\sys\\mvc\\admin\\sys\\generator\\template";
            // 指定路径
            File file = new File(filePath.toString());
            if(file.exists()){//如果已经存在了
            	return EXIST;
            }
            File path = new File(file.getParent());
            if ( ! path.exists() ) {
                path.mkdirs();
            }
            OutputStreamWriter writerStream = new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
            output = new BufferedWriter(writerStream);
//            output = new OutputStreamWriter(new FileWriter(file),"UTF-8");

            // ***
            Engine use = Engine.use();
            use.setBaseTemplatePath(baseTemplatePath);
            Template template = use.getTemplate(templateFileName);
            template.render(kv,output);
//            Engine.use()
//            .setBaseTemplatePath(baseTemplatePath)
//            //.setSourceFactory(new ClassPathSourceFactory())
//            .getTemplate(templateFileName)
//            .render(kv, output);
            // ***
            return SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
            return FAIL;
        }finally{
            try { if( output != null ) output.close(); } catch (IOException e) {}
        }
    }
    
    /**
     * 根据自定义内容生成文件
     * @param data              自定义内容
     * @param filePath          输出目录
     * @return 
     */
    public boolean render(String data, StringBuilder filePath)  {
        BufferedWriter output = null;
        try {
            File file = new File(filePath.toString());
            File path = new File(file.getParent());
            if ( ! path.exists() ) {
                path.mkdirs();
            }
            output = new BufferedWriter(new FileWriter(file));
            output.write(data);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }finally{
            try { if( output != null ) output.close(); } catch (IOException e) {}
        }
    }

}
