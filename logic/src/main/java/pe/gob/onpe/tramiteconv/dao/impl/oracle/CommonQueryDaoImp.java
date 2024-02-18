/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pe.gob.onpe.tramiteconv.dao.impl.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Repository;
import pe.gob.onpe.tramitedoc.bean.CargoFuncionBean;
import pe.gob.onpe.tramitedoc.bean.CiudadanoBean;
import pe.gob.onpe.tramitedoc.bean.DependenciaBean;
import pe.gob.onpe.tramitedoc.bean.DestinatarioOtroOrigenBean;
import pe.gob.onpe.tramitedoc.bean.EmpleadoBean;
import pe.gob.onpe.tramitedoc.bean.EmpleadoVoBoBean;
import pe.gob.onpe.tramitedoc.bean.ProveedorBean;
import pe.gob.onpe.tramitedoc.bean.RemitenteBean;
import pe.gob.onpe.tramitedoc.bean.SiElementoBean;
import pe.gob.onpe.tramitedoc.dao.CommonQueryDao;
import pe.gob.onpe.tramitedoc.dao.SimpleJdbcDaoBase;
import pe.gob.onpe.tramitedoc.web.util.BusquedaTextual;
import pe.gob.onpe.tramitedoc.web.util.Utilidades;
/*interoperabilidad*/
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import pe.gob.onpe.tramitedoc.bean.DatosInterBean;
import pe.gob.onpe.tramitedoc.bean.TablaMaestraBean;
/*interoperabilidad*/
/**
 *
 * @author ECueva
 */
@Repository("commonQueryDao")
public class CommonQueryDaoImp extends SimpleJdbcDaoBase implements CommonQueryDao {

    @Override
    public DependenciaBean getDependenciaxCoDependencia(String coDepen) {
        StringBuffer sql = new StringBuffer();
        DependenciaBean dependenciaBean = null;
        sql.append("SELECT DE_DEPENDENCIA, CO_DEPENDENCIA FROM RHTM_DEPENDENCIA WHERE\n" +
        "CO_DEPENDENCIA=? AND IN_BAJA='0'");
        
        try {
            dependenciaBean = this.jdbcTemplate.queryForObject(sql.toString(), BeanPropertyRowMapper.newInstance(DependenciaBean.class),
                    new Object[]{coDepen});            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dependenciaBean;
    }

    @Override
    public DependenciaBean getDependenciaBasico(String coDepen) {
        StringBuffer sql = new StringBuffer();
        DependenciaBean dependenciaBean = null;
        sql.append("SELECT \n" +
                    "X.DE_DEPENDENCIA, \n" +
                    "X.CO_DEPENDENCIA, \n" +
                    "X.de_sigla,\n" +
                    "X.in_mesa_partes,\n" +
                    "(SELECT DEP.CO_DEPENDENCIA FROM RHTM_DEPENDENCIA DEP WHERE DEP.IN_MESA_PARTES='1' AND DEP.IN_BAJA='0'\n" +
                    "AND 0 < (SELECT COUNT(1) FROM TDTR_DEPENDENCIA_MP DMP WHERE DMP.CO_DEP=X.CO_DEPENDENCIA AND DMP.ES_ELI='0' AND DMP.IN_MP='1')) CO_DEP_MP \n" +
                    "FROM RHTM_DEPENDENCIA X \n" +
                    "WHERE X.CO_DEPENDENCIA=?");
        
        try {
            dependenciaBean = this.jdbcTemplate.queryForObject(sql.toString(), BeanPropertyRowMapper.newInstance(DependenciaBean.class),
                    new Object[]{coDepen});            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dependenciaBean;
    }
    
    @Override
    public List<RemitenteBean> getListRemitente(String coDepen){
        List<RemitenteBean> list = new ArrayList<RemitenteBean>();
        StringBuffer sql = new StringBuffer(); 
        sql.append("select D.DE_DEPENDENCIA descrip, D.CO_DEPENDENCIA cod_dep,D.DE_CORTA_DEPEN de_corta_depen from RHTM_DEPENDENCIA D \n" +
                    "WHERE ? IN (CO_DEPENDENCIA,CO_DEPEN_PADRE) OR \n" +
                    "CO_DEPENDENCIA IN (SELECT co_dep_ref FROM tdtx_referencia \n" +
                    "WHERE co_dep_emi = ?  AND ti_emi = 'D'   AND es_ref = '1') ORDER BY 1");
        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(RemitenteBean.class),
                    new Object[]{coDepen, coDepen});            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    @Override
    public String verificarExpedienteDuplicado(String pnuAnnExp, String pcoDepExp, String pcoGru, String pnuCorrExp){
        String vReturn="1";//duplicado "si".
        try {
            vReturn = this.jdbcTemplate.queryForObject("select count(1) from TDTC_EXPEDIENTE \n" +
                      "WHERE NU_ANN_EXP=? and CO_DEP_EXP=? and CO_GRU=? and NU_CORR_EXP=?", 
                    String.class, new Object[]{pnuAnnExp, pcoDepExp,
                pcoGru, pnuCorrExp});
        } catch (Exception e) {
            e.printStackTrace();
        }        
        return vReturn;
    }
    
    @Override
    public String verficarNumeroDuplicadoEmiDocExtRecep(String pnuAnn,String pcoDepEmi,String pnuCorEmi){
        String vReturn="1";//duplicado "si".
        try {
            vReturn = this.jdbcTemplate.queryForObject("SELECT count(1)\n" +
                                "FROM tdtv_remitos \n" +
                                "WHERE NU_ANN = ?\n" +//:B_02.NU_ANN
                                "AND CO_DEP_EMI = ?\n" +//:B_02.CO_DEP_EMI
                                "AND CO_GRU = '3' \n" +
                                "AND NU_COR_EMI = ?", //:B_02.NU_COR_EMI
                    String.class, new Object[]{pnuAnn, pcoDepEmi,pnuCorEmi});            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn;
    }

    @Override
    public List<ProveedorBean> getLstProveedores(String prazonSocial) {
        StringBuilder sql = new StringBuilder();
       // prazonSocial=Utilidades.fn_getCleanTextLenGreathree(Utilidades.fn_getCleanTextExpReg(prazonSocial));
        
        List<ProveedorBean> list = new ArrayList<ProveedorBean>();
        sql.append("SELECT A.*, ROWNUM\n"
                + "FROM (\n"
                + "SELECT CPRO_RUC NU_RUC, CPRO_RAZSOC DESCRIPCION,CPRO_DOMICIL,CPRO_TELEFO,CPRO_EMAIL, \n"
                + "TRIM(NODEP) NODEP,TRIM(NOPRV) NOPRV,TRIM(NODIS) NODIS, TRIM(CUBI_CODDEP) CUBI_CODDEP,TRIM(CUBI_CODPRO) CUBI_CODPRO,TRIM(CUBI_CODDIS) CUBI_CODDIS,  \n"
                + "NVL(CUBI_CODDEP,'') ID_DEPARTAMENTO, NVL(CUBI_CODPRO,'') ID_PROVINCIA, NVL(CUBI_CODDIS,'') ID_DISTRITO, NVL(CPRO_DOMICIL,'') DE_DIRECCION, NVL(CPRO_EMAIL,'') DE_CORREO,NVL(CPRO_TELEFO,'') TELEFONO \n"
                + "FROM LG_PRO_PROVEEDOR LEFT JOIN IDTUBIAS ON UBDEP||UBPRV||UBDIS=CUBI_CODDEP||CUBI_CODPRO||CUBI_CODDIS\n"
                /*+ "where CONTAINS(cpro_razsoc, '" + BusquedaTextual.getContextValue(prazonSocial) + "'"
                + ", 1 ) > 1\n"
                + "order by score(1) desc \n"
                + ") A WHERE ROWNUM < 51");  */              
                
                //+ "where cpro_ruc='"+prazonSocial+"' or cpro_razsoc like REPLACE('%"+prazonSocial+"%',' ','%') \n"
                + "where cpro_ruc='"+prazonSocial+"' or cpro_razsoc like '%"+prazonSocial +"%' \n" //.replace(' ', '%')
                + " order by 1 desc ) A ");
                //YUAL
                //+ ") A WHERE ROWNUM < 100");       
        
        try {
            if(prazonSocial!=null && prazonSocial.trim().length()>1){            
                list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(ProveedorBean.class),
                        new Object[]{});
            }
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;
    }
    
    @Override
    public List<DestinatarioOtroOrigenBean> getLstOtrosOrigenes(String pdescripcion){
        StringBuilder sql = new StringBuilder();
        //pdescripcion=Utilidades.fn_getCleanTextLenGreathree(Utilidades.fn_getCleanTextExpReg(pdescripcion));        
        
        List<DestinatarioOtroOrigenBean> list = new ArrayList<DestinatarioOtroOrigenBean>();
        sql.append("SELECT DE_APE_PAT_OTR||' '|| DE_APE_MAT_OTR||', '|| DE_NOM_OTR || ' - ' || DE_RAZ_SOC_OTR DESCRIPCION,\n"
                + " PK_SGD_DESCRIPCION.DE_DOMINIOS('TIP_DOC_IDENT', CO_TIP_OTR_ORI ) TIPO_DOC_IDENTIDAD,\n"
                + " NU_DOC_OTR_ORI NRO_DOC_IDENTIDAD, CO_OTR_ORI CO_OTRO_ORIGEN,\n"
                + "NVL(UB_DEP,'') ID_DEPARTAMENTO, NVL(UB_PRO,'') ID_PROVINCIA, NVL(UB_DIS,'') ID_DISTRITO, NVL(DE_DIR_OTRO_ORI,'') DE_DIRECCION, NVL(DE_EMAIL,'') DE_CORREO,NVL(DE_TELEFO,'') TELEFONO \n"
                + "  ,UB_DEP,UB_PRO,UB_DIS\n" 
                + "  ,trim(UB.NODEP)||' '||trim(UB.NOPRV)||' '||trim(UB.NODIS) ubigeo,DE_EMAIL,a.DE_TELEFO,a.DE_DIR_OTRO_ORI\n" 
                + " ,trim(UB.NODEP) NODEP, trim(UB.NOPRV) NOPRV, trim(UB.NODIS) NODIS  \n"
                
                + "  FROM TDTR_OTRO_ORIGEN a\n"
                +" LEFT JOIN IDTUBIAS UB ON UB.UBDEP=a.UB_DEP AND UB.UBPRV=a.UB_PRO AND UB.UBDIS=a.UB_DIS\n" 
                + "where (DE_RAZ_SOC_OTR like '%"+pdescripcion+"%' \n"//-- CONTAINS(in_busca_texto, '"+BusquedaTextual.getContextValue(pdescripcion) +"'\n"
                //+ "--, 1 ) > 1\n"
                + "OR (DE_APE_PAT_OTR||' '|| DE_APE_MAT_OTR||' '||DE_NOM_OTR) like '%"+pdescripcion+"%' \n"
                + "OR (DE_NOM_OTR ||' '||DE_APE_PAT_OTR||' '|| DE_APE_MAT_OTR) like '%"+pdescripcion+"%' \n"
                + "OR NU_DOC_OTR_ORI like REPLACE('%"+pdescripcion+"%',' ','%') \n"
                + ") and nvl(CO_TIP_OTR_ORI,'--') not in('09')\n"
//                + " order by score(1) desc");
                 + " order by 1 desc");
      
      

        
        try {
            if(pdescripcion!=null && pdescripcion.trim().length()>1){
                list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(DestinatarioOtroOrigenBean.class),
                        new Object[]{});
            }
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;
    }
       
    @Override
    public CiudadanoBean getCiudadano(String pnuDoc){
        StringBuffer sql = new StringBuffer();
        sql.append("select I.NULEM NU_DOCUMENTO,substr(RTRIM(I.DEAPP)||' '||RTRIM(I.DEAPM)||' '||RTRIM(I.DENOM),1,200) NOMBRE,\n"
                + "NVL(I.UBDEP,'') ID_DEPARTAMENTO, NVL(I.UBPRV,'') ID_PROVINCIA, NVL(I.UBDIS,'') ID_DISTRITO, NVL(I.DEDOMICIL,'') DE_DIRECCION, NVL(I.DEEMAIL,'') DE_CORREO,NVL(I.DETELEFO,'') TELEFONO \n"
                + ",I.UBDEP, I.UBPRV,I.UBDIS,I.DEDOMICIL,I.DEEMAIL, I.DETELEFO ,  trim(UB.NODEP)||'/'||trim(UB.NOPRV)||'/'||trim(UB.NODIS) ubigeo \n"
                + "from TDTX_ANI_SIMIL I LEFT JOIN IDTUBIAS UB ON UB.UBDEP=I.UBDEP AND UB.UBPRV=I.UBPRV AND UB.UBDIS=I.UBDIS \n"
                + "where\n"
                + "NULEM like ?||'%'\n"
                + "order by 2");
        

        CiudadanoBean ciudadanoBean = new CiudadanoBean();
        try {
            ciudadanoBean = this.jdbcTemplate.queryForObject(sql.toString(), BeanPropertyRowMapper.newInstance(CiudadanoBean.class),
                    new Object[]{pnuDoc});
        } catch (EmptyResultDataAccessException e) {
            ciudadanoBean = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ciudadanoBean;
    }  
    
    @Override
    public ProveedorBean getProveedor(String pnuRuc){
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT CPRO_RUC NU_RUC, CPRO_RAZSOC DESCRIPCION, \n"
                + "NVL(CUBI_CODDEP,'') ID_DEPARTAMENTO, NVL(CUBI_CODPRO,'') ID_PROVINCIA, NVL(CUBI_CODDIS,'') ID_DISTRITO, NVL(CPRO_DOMICIL,'') DE_DIRECCION, NVL(CPRO_EMAIL,'') DE_CORREO,NVL(CPRO_TELEFO,'') TELEFONO \n"
                +"FROM LG_PRO_PROVEEDOR\n"
                +"where CPRO_RUC=?");

        ProveedorBean proveedorBean=null;
        try {
            proveedorBean = this.jdbcTemplate.queryForObject(sql.toString(), BeanPropertyRowMapper.newInstance(ProveedorBean.class),
                    new Object[]{pnuRuc});
        } catch (EmptyResultDataAccessException e) {
            proveedorBean = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proveedorBean;
    }
    
    @Override
    public List<EmpleadoBean> getLsEmpleado(String coDep){
        StringBuffer sql = new StringBuffer();
        List<EmpleadoBean> list;
        sql.append("SELECT e.cemp_apepat||' '||e.cemp_apemat||' '||e.cemp_denom nombre, e.CEMP_CODEMP\n" +
            "FROM RHTM_PER_EMPLEADOS e\n" +
            "where e.CEMP_EST_EMP = '1'\n" +
            "ORDER BY 1");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(EmpleadoBean.class),
                    new Object[]{});
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;        
    }
    
  
    @Override
    public List<CargoFuncionBean> getLsEmpleo(){
        StringBuffer sql = new StringBuffer();
        List<CargoFuncionBean> list;
        sql.append("SELECT CCAR_DESCAR DESCRIPCION, CCAR_CO_CARGO CODIGO\n" +
                    "FROM RHTM_CARGOS\n" +
                    "WHERE CCAR_CO_CARGO <> '000'\n" +
                    "ORDER BY 1");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(CargoFuncionBean.class),
                    new Object[]{});
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;        
    }    
    
    @Override
    public List<DependenciaBean> getLsDepencia(){
        StringBuffer sql = new StringBuffer();
        List<DependenciaBean> list;
        sql.append("SELECT DE_DEPENDENCIA,\n" +
                    "       CO_DEPENDENCIA,\n" +
                    "       DE_CORTA_DEPEN\n" +
                    "  FROM RHTM_DEPENDENCIA\n" +
                    " WHERE co_nivel <> '6'\n" +
                    "   AND IN_BAJA = '0'\n" +
                    "   order by 1");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(DependenciaBean.class),
                    new Object[]{});
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;        
    }
    
    @Override
    public String[] getNroCorrLocalDependencia(){
        String vReturn = "ERROR|";
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT NVL( MAX( a.NU_CORRELATIVO) , 0)+1\n" +
                    "FROM SITM_LOCAL_DEPENDENCIA a");

        try {
            String nroCorr = this.jdbcTemplate.queryForObject(sql.toString(), String.class);
            vReturn="OK|"+nroCorr;
        } catch (EmptyResultDataAccessException e) {
            vReturn="OK|1";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn.split("\\|");         
    }
    
    @Override
    public List<EmpleadoBean> getLsEmpleadoIntitu(String deEmp){
        StringBuffer sql = new StringBuffer();
        List<EmpleadoBean> list;
        sql.append("SELECT e.cemp_apepat||' '||e.cemp_apemat||' '||e.cemp_denom nombre, e.CEMP_CODEMP\n" +
            "FROM RHTM_PER_EMPLEADOS e\n" +
            "where e.CEMP_EST_EMP = '1' \n" +
            "and  e.cemp_apepat||' '||e.cemp_apemat||' '||e.cemp_denom LIKE '%'||?||'%'\n" +
            "ORDER BY 1");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(EmpleadoBean.class),
                    new Object[]{deEmp});
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;        
    }
    
    @Override
    public String[] getCodigoMotivoDocRec(String nuAnn,String nuEmi,String nuDes){
        String vReturn = "ERROR|";
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT CO_MOT FROM TDTV_DESTINOS\n" +
                    "WHERE NU_ANN=?\n" +
                    "AND NU_EMI=?\n" +
                    "AND NU_DES=?\n" +
                    "AND ES_ELI='0'");

        try {
            String coMot = this.jdbcTemplate.queryForObject(sql.toString(), String.class, new Object[]{nuAnn,nuEmi,nuDes});
            vReturn="OK|"+coMot;
        } catch (EmptyResultDataAccessException e) {
            vReturn="ERROR|";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn.split("\\|");               
    }
    
    @Override
    public EmpleadoBean getEmpJefeDep(String coDep){
        StringBuffer sql = new StringBuffer();
        sql.append("select CO_EMPLEADO CEMP_CODEMP,PK_SGD_DESCRIPCION.DE_NOM_EMP(CO_EMPLEADO) NOMBRE\n" +
                    "from rhtm_dependencia\n" +
                    "where CO_DEPENDENCIA = ?\n" +
                    "AND IN_BAJA='0'");

        EmpleadoBean empleadoBean = new EmpleadoBean();
        try {
            empleadoBean = this.jdbcTemplate.queryForObject(sql.toString(), BeanPropertyRowMapper.newInstance(EmpleadoBean.class),
                    new Object[]{coDep});
        } catch (EmptyResultDataAccessException e) {
            empleadoBean = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return empleadoBean;        
    }
    
    @Override
    public List<EmpleadoBean> getLsEmpDepDesEmp(String deEmp,String coDep){
        StringBuffer sql = new StringBuffer();
        List<EmpleadoBean> list;
        sql.append("SELECT e.cemp_apepat||' '||e.cemp_apemat||' '||e.cemp_denom nombre, e.CEMP_CODEMP \n" +
                    "FROM RHTM_PER_EMPLEADOS e, \n" +
                    "( \n" +
                    "SELECT CEMP_CODEMP \n" +
                    "FROM RHTM_PER_EMPLEADOS \n" +
                    "where CEMP_EST_EMP = '1' \n" +
                    "  and (CO_DEPENDENCIA=? or CEMP_CO_DEPEND=?) \n" +
                    "union \n" +
                    "select co_emp from tdtx_dependencia_empleado  where co_dep=? and es_emp='0' \n" +
                    "union \n" +
                    "select co_empleado from rhtm_dependencia where co_dependencia=? \n" +
                    ") a \n" +
                    "where e.cemp_codemp = a.cemp_codemp\n" +
                    "and  e.cemp_apepat||' '||e.cemp_apemat||' '||e.cemp_denom LIKE '%'||?||'%'\n" +
                    "order by 1");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(EmpleadoBean.class),
                    new Object[]{coDep,coDep,coDep,coDep,deEmp});
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;        
    }
    
    @Override
    public List<EmpleadoVoBoBean> getLsPersonalVoBo(String nuAnn,String nuEmi){
        StringBuffer sql = new StringBuffer();
        List<EmpleadoVoBoBean> list;

        sql.append("SELECT VB.CO_EMP_VB CO_EMPLEADO,(SELECT CEMP_APEPAT || ' ' || CEMP_APEMAT || ' ' || CEMP_DENOM FROM RHTM_PER_EMPLEADOS WHERE CEMP_CODEMP = VB.CO_EMP_VB) NOMBRE,\n" +
                    "VB.CO_DEP CO_DEPENDENCIA,PK_SGD_DESCRIPCION.DE_SIGLA(VB.CO_DEP) DE_DEPENDENCIA,DECODE(VB.IN_VB,'B','0',VB.IN_VB) IN_VOBO\n" +
                    "FROM TDTV_PERSONAL_VB VB\n" +
                    "WHERE VB.NU_ANN=?\n" +
                    "AND VB.NU_EMI=?\n" +
                    "ORDER BY NOMBRE");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(EmpleadoVoBoBean.class),
                    new Object[]{nuAnn,nuEmi});
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;
    }
    
    @Override
    public String getInFirmaDoc(String pcoDep,String pcoTipoDoc){
        StringBuffer sql = new StringBuffer();
        String result = null;
        sql.append(" SELECT DECODE(es_obl_firma,'0','N','F')\n" +
                    "FROM sitm_doc_dependencia\n" +
                    "WHERE co_dep     = ?\n" +
                    "AND co_tip_doc = ?");        
        try {
            result = this.jdbcTemplate.queryForObject(sql.toString(), String.class, new Object[]{pcoDep, pcoTipoDoc});
        } catch (EmptyResultDataAccessException e) {
            result = "F";
        } catch (Exception e) {
            result = "F";
            e.printStackTrace();
        }
        
        return result;        
    }
    
    @Override
    public List<CiudadanoBean> getLstCiudadanos(String sDescCiudadano){
        List<CiudadanoBean> list = new ArrayList<CiudadanoBean>();
        StringBuffer sql = new StringBuffer();
        
//        if(sDescCiudadano != null){
//           sDescCiudadano = sDescCiudadano.replace(" ", "%"); 
//        }
        sql.append("select I.NULEM NU_DOCUMENTO,substr(RTRIM(I.DEAPP)||' '||RTRIM(I.DEAPM)||' '||RTRIM(I.DENOM),1,200) NOMBRE,\n"
                + "NVL(UBDEP,'') ID_DEPARTAMENTO, NVL(UBPRV,'') ID_PROVINCIA, NVL(UBDIS,'') ID_DISTRITO, NVL(DEDOMICIL,'') DE_DIRECCION, NVL(DEEMAIL,'') DE_CORREO,NVL(DETELEFO,'') TELEFONO \n"
                + "from IDTANIRS I \n"
                + "where\n"
                + "(substr(RTRIM(I.DEAPP)||' '||RTRIM(I.DEAPM)||' '||RTRIM(I.DENOM),1,200)) like '%'||?||'%'\n"
//                + "(substr(RTRIM(I.DEAPP)||' '||RTRIM(I.DEAPM)||' '||RTRIM(I.DENOM),1,200)) like '%'||?||'%' OR\n"
//                + "(substr(RTRIM(I.DEAPP)||' '||RTRIM(I.DENOM)||' '||RTRIM(I.DEAPM),1,200)) like '%'||?||'%' OR\n"
//                + "(substr(RTRIM(I.DEAPM)||' '||RTRIM(I.DEAPP)||' '||RTRIM(I.DENOM),1,200)) like '%'||?||'%' OR\n"
//                + "(substr(RTRIM(I.DENOM)||' '||RTRIM(I.DEAPP)||' '||RTRIM(I.DEAPM),1,200)) like '%'||?||'%' OR\n"
//                + "(substr(RTRIM(I.DEAPM)||' '||RTRIM(I.DENOM)||' '||RTRIM(I.DEAPP),1,200)) like '%'||?||'%' OR\n"
//                + "(substr(RTRIM(I.DENOM)||' '||RTRIM(I.DEAPM)||' '||RTRIM(I.DEAPP),1,200)) like '%'||?||'%' \n"
                + "order by 2");

        CiudadanoBean ciudadanoBean = new CiudadanoBean();
        try {
              list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(CiudadanoBean.class),
                    new Object[]{sDescCiudadano.toUpperCase()});
//            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(CiudadanoBean.class),
//                    new Object[]{sDescCiudadano.toUpperCase(), sDescCiudadano.toUpperCase(), sDescCiudadano.toUpperCase(),
//                                 sDescCiudadano.toUpperCase(), sDescCiudadano.toUpperCase(), sDescCiudadano.toUpperCase()});
        } catch (EmptyResultDataAccessException e) {
            ciudadanoBean = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
     @Override
    public List<SiElementoBean> getLsDepenciaMensjeria(){
        StringBuffer sql = new StringBuffer();
        List<SiElementoBean> list;
        sql.append("SELECT CELE_DESELE,\n" +
                    "CELE_DESCOR \n" +
                    "FROM SI_ELEMENTO\n" +
                    "WHERE SI_ELEMENTO.CTAB_CODTAB='MSJ_DEPENDENCIA'\n" +
                    "order by SI_ELEMENTO.NELE_NUMSEC");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(SiElementoBean.class));
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;        
    }
    
    
     @Override
    public String obtenerValorParametro(String nombreParametro){
        StringBuffer sql = new StringBuffer();
        String result = null;
        sql.append("SELECT UPPER(DE_PAR) DE_PAR FROM TDTR_PARAMETROS WHERE CO_PAR=?");        
        try {
            result = this.jdbcTemplate.queryForObject(sql.toString(), String.class, new Object[]{nombreParametro});
        } catch (EmptyResultDataAccessException e) {
            result = "";
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;        
    }
    
    @Override
    public String getCargoEmpleado(String coDep, String coEmp){
        StringBuilder sql = new StringBuilder();
        String result = null;
        sql.append("SELECT PK_SGD_DESCRIPCION.DE_CARGO_DOCUMENTO(?, ?) FROM DUAL");        
        try {
            result = this.jdbcTemplate.queryForObject(sql.toString(), String.class, new Object[]{coDep, coEmp});
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;        
    }

    /*interoperabilidad*/
    @Override
    public DatosInterBean DatosInter(String nuAnn, String nuEmi) {
        
        DatosInterBean datosInter=new DatosInterBean();
        
        StringBuffer sql = new StringBuffer();
        
        sql.append("SELECT (CASE WHEN LENGTH(A.NU_RUC_DES)>0 THEN A.NU_RUC_DES ELSE '0' END) AS NU_RUC_DES,"+
                "(CASE WHEN LENGTH(A.DE_NOM_DES)>0 THEN a.DE_NOM_DES ELSE \n" +
                " CASE WHEN LENGTH(A.REMI_NU_DNI_EMI)>0 THEN PK_SGD_DESCRIPCION.ANI_SIMIL(a.REMI_NU_DNI_EMI) ELSE \n" +
                " CASE WHEN LENGTH(A.REMI_CO_OTR_ORI_EMI)>0 THEN PK_SGD_DESCRIPCION.OTRO_ORIGEN(a.remi_co_otr_ori_emi) ELSE '' END END END) DE_NOM_DES,\n"+
                "(CASE WHEN LENGTH(A.DE_DEP_DES)>0 THEN DE_DEP_DES ELSE '' END) DE_DEP_DES,\n"+
                "(CASE WHEN LENGTH(A.DE_CAR_DES)>0 THEN DE_CAR_DES ELSE A.DE_CARGO END) DE_CAR_DES FROM TDTV_DESTINOS A INNER JOIN TDTV_REMITOS B ON A.NU_ANN=B.NU_ANN AND A.NU_EMI=B.NU_EMI \n" +
                "WHERE A.NU_ANN=?\n" +
                "AND A.NU_EMI=?\n" +
                "AND A.NU_DES=1 AND B.CO_TIP_DOC_ADM IN (SELECT CDOC_TIPDOC FROM IOTDTX_TIPO_DOCUMENTO)");

        try {
             datosInter = this.jdbcTemplate.queryForObject(sql.toString(), BeanPropertyRowMapper.newInstance(DatosInterBean.class), new Object[]{nuAnn,nuEmi});
//            vReturn="OK|"+coMot;
        } catch (EmptyResultDataAccessException e) {
//            vReturn="ERROR|";
            datosInter=null;
        } catch (Exception e) {
            e.printStackTrace();
            datosInter=null;
        }
        
        return datosInter;
    }
    
    /*interoperabilidad*/

    //YUAL
  @Override
  public List<TablaMaestraBean> getLsContenidoTabla(String vTabla){
        StringBuffer sql = new StringBuffer();
        List<TablaMaestraBean> list;
         
        sql.append("SELECT column_name as Columna, data_type as TipoDato, data_length  as Tamanio \n" +
                    " FROM all_tab_columns \n" +
                    "WHERE table_name = ?\n");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(TablaMaestraBean.class),
                    new Object[]{vTabla});
            
            /*GENERANDO LA CONSULTA*/
            String query="SELECT '<tr>'||";            
            
            for(int i=0; i<list.size();i++){
             if(list.get(i).getTipoDato().equals("VARCHAR")||list.get(i).getTipoDato().equals("VARCHAR2")||list.get(i).getTipoDato().equals("CHAR"))   
             {
                 query=query+"'<td><input id=''"+list.get(i).getColumna()+i+"'' >'||"+list.get(i).getColumna()+"||'</input></td>'";
             }
            
             
            }
            
            query=query+" ||'</tr>' as Valor FROM IDOSGD."+vTabla; 
            
            list=null;
            list = this.jdbcTemplate.query(query.toString(), BeanPropertyRowMapper.newInstance(TablaMaestraBean.class));
            
            
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;
    }
  
  
  //YUAL
     @Override
    public List<CargoFuncionBean> getCargoBusqList(CargoFuncionBean cargo) {
        StringBuffer sql = new StringBuffer();
        Map<String, Object> objectParam = new HashMap<String, Object>();
        
        sql.append("SELECT  CCAR_CO_CARGO AS codigo, ");
        sql.append("        CCAR_DESCAR AS descripcion, ");
        sql.append("        CCAR_EST_CAR as estado ");        
        sql.append("FROM RHTM_CARGOS ");
        sql.append("WHERE 1 = 1 ");
        
         if (cargo.getCodigo() != null && !cargo.getCodigo().equals("")) {
            sql.append("AND CCAR_CO_CARGO='"+cargo.getCodigo()+"' "); 
            
        }
         
        if (cargo.getDescripcion() != null && !cargo.getDescripcion().equals("")) {
            sql.append("AND UPPER(CCAR_DESCAR) like '%"+cargo.getDescripcion()+"%' "); 
            
        }

        if (cargo.getEstado() != null && !cargo.getEstado().equals("")) {
            sql.append("AND CCAR_EST_CAR ="+cargo.getEstado()); 
            //objectParam.put("estado", cargo.getEstado());
        }
        sql.append(" ORDER BY CCAR_CO_CARGO DESC ");
        
        List<CargoFuncionBean> list = new ArrayList<CargoFuncionBean>();
        try {
            list = this.namedParameterJdbcTemplate.query(sql.toString(), objectParam, BeanPropertyRowMapper.newInstance(CargoFuncionBean.class));            
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public String insCargo(CargoFuncionBean cargo, String codusuario) {
        String vReturn = "NO_OK";
        StringBuffer sql = new StringBuffer();
        String genCoCargo = "";
        
         if (cargo.getCodigo() != null && !cargo.getCodigo().equals("")) {          
                sql.append("UPDATE RHTM_CARGOS SET ");
                sql.append("CCAR_EST_CAR=?, CCAR_DESCAR=? ");
                sql.append("WHERE CCAR_CO_CARGO=? ");
                genCoCargo=cargo.getCodigo();
        }
        else
         {
                sql.append("INSERT INTO RHTM_CARGOS ");
                sql.append("( CCAR_EST_CAR,CCAR_DESCAR,CCAR_CO_CARGO) ");
                sql.append("values (?, ?, ?) ");
                  //El id de la tabla sera autogenerado sumando 1 al ultimo ID
            String seq = "SELECT LPAD(NVL(MAX(TO_NUMBER(CCAR_CO_CARGO)), 0) + 1, 3, 0) FROM RHTM_CARGOS ";
            genCoCargo = (String) this.jdbcTemplate.queryForObject(seq, String.class);
         }
        
        try {
          
            
            //El campo Estado no se incluye en el bloque del insert pq es un valor default en la tabla
            this.jdbcTemplate.update(sql.toString(), new Object[]{
                 cargo.getEstado(),cargo.getDescripcion(),genCoCargo
            });
            
            vReturn = "OK";
            
        } catch (DuplicateKeyException con) {
            vReturn = "Número de Codigo de cargo duplicado.";
            con.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn;
    }

  //END YUAL
   @Override
    public List<SiElementoBean> getLsBuscardoExpIntExt(String codigoEmpleado){
        StringBuffer sql = new StringBuffer();
        List<SiElementoBean> list;
        sql.append("SELECT CELE_DESELE,\n" +
                    "CELE_DESCOR \n" +
                    "FROM SI_ELEMENTO\n " +
                    "WHERE SI_ELEMENTO.CTAB_CODTAB='BUSQ_EXPE_EXT_INT' AND CELE_DESCOR='"+codigoEmpleado+"' \n" +
                    "order by SI_ELEMENTO.NELE_NUMSEC");

        try {
            list = this.jdbcTemplate.query(sql.toString(), BeanPropertyRowMapper.newInstance(SiElementoBean.class));
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            //e.printStackTrace();
        }
        return list;        
    }
}