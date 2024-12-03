
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 罚金信息
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/fajin")
public class FajinController {
    private static final Logger logger = LoggerFactory.getLogger(FajinController.class);

    private static final String TABLE_NAME = "fajin";

    @Autowired
    private FajinService fajinService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private NewsService newsService;//公告信息
    @Autowired
    private PandianService pandianService;//每日盘点
    @Autowired
    private ShujiService shujiService;//书籍
    @Autowired
    private ShujiChuruInoutService shujiChuruInoutService;//出入库
    @Autowired
    private ShujiChuruInoutListService shujiChuruInoutListService;//出入库详情
    @Autowired
    private ShujiCollectionService shujiCollectionService;//书籍收藏
    @Autowired
    private ShujiCommentbackService shujiCommentbackService;//书籍评价
    @Autowired
    private ShujiYuyueService shujiYuyueService;//书籍借阅
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = fajinService.queryPage(params);

        //字典表数据转换
        List<FajinView> list =(List<FajinView>)page.getList();
        for(FajinView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        FajinEntity fajin = fajinService.selectById(id);
        if(fajin !=null){
            //entity转view
            FajinView view = new FajinView();
            BeanUtils.copyProperties( fajin , view );//把实体数据重构到view中
            //级联表 用户
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(fajin.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody FajinEntity fajin, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,fajin:{}",this.getClass().getName(),fajin.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            fajin.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<FajinEntity> queryWrapper = new EntityWrapper<FajinEntity>()
            .eq("yonghu_id", fajin.getYonghuId())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        FajinEntity fajinEntity = fajinService.selectOne(queryWrapper);
        if(fajinEntity==null){
            fajin.setInsertTime(new Date());
            fajin.setCreateTime(new Date());
            fajinService.insert(fajin);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody FajinEntity fajin, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,fajin:{}",this.getClass().getName(),fajin.toString());
        FajinEntity oldFajinEntity = fajinService.selectById(fajin.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            fajin.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

            fajinService.updateById(fajin);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<FajinEntity> oldFajinList =fajinService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        fajinService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //.eq("time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
        try {
            List<FajinEntity> fajinList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            FajinEntity fajinEntity = new FajinEntity();
//                            fajinEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            fajinEntity.setFajinPrice(data.get(0));                    //缴纳金额 要改的
//                            fajinEntity.setFajinContent("");//详情和图片
//                            fajinEntity.setInsertTime(date);//时间
//                            fajinEntity.setCreateTime(date);//时间
                            fajinList.add(fajinEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        fajinService.insertBatch(fajinList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = fajinService.queryPage(params);

        //字典表数据转换
        List<FajinView> list =(List<FajinView>)page.getList();
        for(FajinView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        FajinEntity fajin = fajinService.selectById(id);
            if(fajin !=null){


                //entity转view
                FajinView view = new FajinView();
                BeanUtils.copyProperties( fajin , view );//把实体数据重构到view中

                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(fajin.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody FajinEntity fajin, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,fajin:{}",this.getClass().getName(),fajin.toString());
        Wrapper<FajinEntity> queryWrapper = new EntityWrapper<FajinEntity>()
            .eq("yonghu_id", fajin.getYonghuId())
//            .notIn("fajin_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        FajinEntity fajinEntity = fajinService.selectOne(queryWrapper);
        if(fajinEntity==null){
            fajin.setInsertTime(new Date());
            fajin.setCreateTime(new Date());
        fajinService.insert(fajin);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

