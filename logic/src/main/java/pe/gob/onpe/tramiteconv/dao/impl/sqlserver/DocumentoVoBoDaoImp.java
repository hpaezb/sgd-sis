/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pe.gob.onpe.tramiteconv.dao.impl.sqlserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Repository;
import pe.gob.onpe.tramitedoc.bean.BuscarDocumentoVoBoBean;
import pe.gob.onpe.tramitedoc.bean.DocumentoVoBoBean;
import pe.gob.onpe.tramitedoc.dao.DocumentoVoBoDao;
import pe.gob.onpe.tramitedoc.dao.SimpleJdbcDaoBase;
import pe.gob.onpe.tramitedoc.web.util.BusquedaTextual;

/**
 *
 * @author ecueva
 */
@Repository("documentoVoboDao")
public class DocumentoVoBoDaoImp extends SimpleJdbcDaoBase implements DocumentoVoBoDao{

    @Override
    public List<DocumentoVoBoBean> getLsDocsVoBo(BuscarDocumentoVoBoBean bDocVoBo) {
        StringBuffer sql = new StringBuffer();
        boolean bBusqFiltro = false;
        String sOrdenList="";
        String sqlContains = "";
        Map<String, Object> objectParam = new HashMap<String, Object>();

        List<DocumentoVoBoBean> list;

        sql.append("SELECT TOP 100 A.* ");
        sql.append(" FROM ( ");
        sql.append("	SELECT RE.NU_ANN, RE.NU_EMI, RE.NU_COR_EMI, CONVERT(VARCHAR(10), RE.FE_EMI, 3) FE_EMI_CORTA, RE.FE_EMI, ");
        sql.append("    (SELECT RETORNO FROM [IDOSGD].[PK_SGD_DESCRIPCION_DE_EMI_REF](RE.NU_ANN, RE.NU_EMI)) DE_EMI_REF, ");
        sql.append("	(SELECT CDOC_DESDOC FROM IDOSGD.SI_MAE_TIPO_DOC WHERE CDOC_TIPDOC = RE.CO_TIP_DOC_ADM) DE_TIP_DOC_ADM,RR.NU_DOC, UPPER(SUBSTRING(RE.DE_ASU,0,399)) DE_ASU,");
        sql.append("	(SELECT CEMP_APEPAT + ' ' + CEMP_APEMAT + ' ' + CEMP_DENOM FROM IDOSGD.RHTM_PER_EMPLEADOS WHERE CEMP_CODEMP = VB.CO_EMP_VB)  DE_EMP_VB,");
        sql.append("	(SELECT CEMP_APEPAT + ' ' + CEMP_APEMAT + ' ' + CEMP_DENOM FROM IDOSGD.RHTM_PER_EMPLEADOS WHERE CEMP_CODEMP = RE.CO_EMP_RES) DE_EMP_ELABORA,");
        sql.append("	RR.NU_EXPEDIENTE NRO_EXPEDIENTE,");
        sql.append("	(SELECT DE_EST FROM IDOSGD.TDTR_ESTADOS WHERE (CO_EST + DE_TAB) = (CASE VB.IN_VB WHEN 'B' THEN '0' ELSE VB.IN_VB END) + 'TDTV_PERSONAL_VB') DE_ES_DOC_EMI,");
        sql.append("	RR.IN_EXISTE_DOC EXISTE_DOC, (CASE ISNULL(RR.TI_EMI_REF, '0') + ISNULL(RR.IN_EXISTE_ANEXO, '2') WHEN '00' THEN 0 WHEN '02' THEN 0 ELSE 1 END) EXISTE_ANEXO ");
        sql.append("	FROM IDOSGD.TDTV_REMITOS RE WITH (NOLOCK) , IDOSGD.TDTV_PERSONAL_VB VB WITH (NOLOCK) , IDOSGD.TDTX_REMITOS_RESUMEN RR WITH (NOLOCK) ");
        sql.append("	WHERE RE.NU_ANN=VB.NU_ANN");
        sql.append("	AND RE.NU_EMI=VB.NU_EMI");
        sql.append("	AND RR.NU_ANN=RE.NU_ANN");
        sql.append("	AND RR.NU_EMI=RE.NU_EMI");
        sql.append("	AND (RR.TI_EMI='01' OR RR.TI_EMI='05')");
        sql.append("	AND (RE.TI_EMI='01' OR RR.TI_EMI='05') ");
        sql.append("	AND (RE.ES_DOC_EMI='7' OR (RE.TI_EMI='05' AND RE.ES_DOC_EMI='5'))");
        sql.append("	AND RE.ES_ELI='0'");
        sql.append("	AND (RE.CO_GRU='1' OR RE.CO_GRU='2')");
        //sql.append("	AND VB.IN_VB='0'");
        String pnuAnn = bDocVoBo.getNuAnn();
        if (pnuAnn!=null&&pnuAnn.trim().length()==4) {
            sql.append(" AND RE.NU_ANN = :pnuAnn");
            objectParam.put("pnuAnn", pnuAnn);
        }
        sql.append(" AND VB.CO_DEP = :pCoDep");

        // Parametros Basicos
        objectParam.put("pCoDep", bDocVoBo.getCoDepUsu());

        String pTipoBusqueda = bDocVoBo.getTipBusqueda();
        if (pTipoBusqueda.equals("1")&&bDocVoBo.isEsIncluyeFiltro()) {
            bBusqFiltro = true;
        }

        sql.append(" AND VB.CO_EMP_VB = :pcoEmpVobo ");
        objectParam.put("pcoEmpVobo", bDocVoBo.getCoEmpUsu());
        
        //Filtro
        if (pTipoBusqueda.equals("0") || bBusqFiltro) {
            if (bDocVoBo.getTipDoc()!= null && bDocVoBo.getTipDoc().trim().length() > 0) {
                sql.append(" AND RE.CO_TIP_DOC_ADM = :pCoDocEmi ");
                objectParam.put("pCoDocEmi", bDocVoBo.getTipDoc());
            }
            if (bDocVoBo.getEsDoc()!= null && bDocVoBo.getEsDoc().trim().length() > 0) {
                sql.append(" AND (VB.IN_VB = :pEsDocEmi ");
                String esDoc=bDocVoBo.getEsDoc();
                if(esDoc.equals("0")){
                   sql.append(" OR VB.IN_VB = 'B') "); 
                }else{
                   sql.append(") ");  
                }
                objectParam.put("pEsDocEmi", bDocVoBo.getEsDoc());
            }else{
                sOrdenList=" DESC";
            }
            if (bDocVoBo.getCoPrioridadDoc()!= null && bDocVoBo.getCoPrioridadDoc().trim().length() > 0) {
                sql.append(" AND RR.CO_PRIORIDAD = :pCoPrioridad ");
                objectParam.put("pCoPrioridad", bDocVoBo.getCoPrioridadDoc());
            }
            if (bDocVoBo.getCoRefOri()!= null && bDocVoBo.getCoRefOri().trim().length() > 0) {
                sql.append(" AND CHARINDEX(:pTiEmiRef,RR.TI_EMI_REF) > 0 ");
                objectParam.put("pTiEmiRef", bDocVoBo.getCoRefOri());
            }
            if (bDocVoBo.getCoEmpElabora()!= null && bDocVoBo.getCoEmpElabora().trim().length() > 0) {
                sql.append(" AND RE.CO_EMP_RES = :pcoEmpEla ");
                objectParam.put("pcoEmpEla", bDocVoBo.getCoEmpElabora());                            
            }
            String vFeEmiIni = bDocVoBo.getFeIni();
            String vFeEmiFin = bDocVoBo.getFeFin();
            if (vFeEmiIni != null && vFeEmiIni.trim().length() == 10
                    && vFeEmiFin != null && vFeEmiFin.trim().length() == 10) { 
                sql.append(" AND RE.FE_EMI BETWEEN CONVERT(DATETIME, :pFeEmiIni, 103) AND CONVERT(DATETIME, :pFeEmiFin, 103) + 0.99999 ");
                objectParam.put("pFeEmiIni", vFeEmiIni);
                objectParam.put("pFeEmiFin", vFeEmiFin);
            }
        }

        //Busqueda
        if (pTipoBusqueda.equals("1")) {
            if (bDocVoBo.getNuCorEmi()!= null && bDocVoBo.getNuCorEmi().trim().length() > 0) {
                sql.append(" AND RE.NU_COR_EMI = :pnuCorEmi ");
                objectParam.put("pnuCorEmi", bDocVoBo.getNuCorEmi());
            }

            if (bDocVoBo.getNuDoc() != null && bDocVoBo.getNuDoc().trim().length() > 1) {
                sql.append(" AND RR.NU_DOC LIKE ''+:pnuDocEmi+'%' ");
                objectParam.put("pnuDocEmi", bDocVoBo.getNuDoc());
            }

            if (bDocVoBo.getNroExp() != null && bDocVoBo.getNroExp().trim().length() > 1) {
                sql.append(" AND RR.NU_EXPEDIENTE LIKE '%'+:pnuExpediente+'%' ");
                objectParam.put("pnuExpediente", bDocVoBo.getNroExp());
            }

            // Busqueda del Asunto
            if (bDocVoBo.getAsunto()!= null && bDocVoBo.getAsunto().trim().length() > 1) {
//                sql.append(" AND CONTAINS(RE.*, :pBusquedaTextual) ");
//                sqlContains = "SELECT RETORNO FROM [IDOSGD].PK_SGD_DESCRIPCION_QUERYFILTER('"+bDocVoBo.getAsunto()+"')";
                  sql.append(" AND UPPER(RE.DE_ASU) LIKE '%'+UPPER(:pDeAsunto)+'%' ");
                   objectParam.put("pDeAsunto", bDocVoBo.getAsunto());
            }
        }
        sql.append(") AS A ORDER BY FE_EMI ").append(sOrdenList);

        try {
            //Obteniendo el parametro textual si es requerido
//            if (sqlContains.length() > 0) {
//                String cadena = this.jdbcTemplate.queryForObject(sqlContains, String.class);            
//                objectParam.put("pBusquedaTextual", cadena);
//            }
            list = this.namedParameterJdbcTemplate.query(sql.toString(), objectParam, BeanPropertyRowMapper.newInstance(DocumentoVoBoBean.class));
        } catch (EmptyResultDataAccessException e) {
            list = null;
        } catch (Exception e) {
            list = null;
            e.printStackTrace();
        }
        return list;
    }
    
    @Override
    public DocumentoVoBoBean getDocumentoVoBo(String pnuAnn, String pnuEmi, String coDep, String coEmpVb) {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ");
        sql.append("RE.NU_ANN, ");
        sql.append("RE.NU_EMI, ");
        sql.append("RE.NU_COR_EMI, ");
        sql.append("[IDOSGD].[PK_SGD_DESCRIPCION_DE_DEPENDENCIA](RE.CO_DEP_EMI) DE_DEP_EMI, ");
        sql.append("(SELECT CEMP_APEPAT + ' ' + CEMP_APEMAT + ' ' + CEMP_DENOM FROM IDOSGD.RHTM_PER_EMPLEADOS WHERE CEMP_CODEMP = RE.CO_EMP_EMI) DE_EMP_EMI, ");
        sql.append("(SELECT CEMP_APEPAT + ' ' + CEMP_APEMAT + ' ' + CEMP_DENOM FROM IDOSGD.RHTM_PER_EMPLEADOS WHERE CEMP_CODEMP = RE.CO_EMP_RES) DE_EMP_ELABORA, ");
        sql.append("CONVERT(VARCHAR(10), RE.FE_EMI, 103) FE_EMI_CORTA, ");
        sql.append("[IDOSGD].[PK_SGD_DESCRIPCION_DE_DOCUMENTO](RE.CO_TIP_DOC_ADM) DE_TIP_DOC_ADM, ");
        sql.append("CASE RE.TI_EMI "); 
        sql.append("	 WHEN '01' THEN ISNULL(RE.NU_DOC_EMI, ' ') + '-' + RE.NU_ANN + '-' + RE.DE_DOC_SIG "); 
        sql.append("	 WHEN '05' THEN ISNULL(RE.NU_DOC_EMI, ' ') + '-' + RE.NU_ANN + '-' + RE.DE_DOC_SIG ");
        sql.append("	 ELSE ISNULL(RE.DE_DOC_SIG, 'S/N') ");
        sql.append("END NU_DOC, "); 		
        sql.append("CASE VB.IN_VB WHEN 'B' THEN '0' ELSE VB.IN_VB END ES_DOC_EMI, ");
        sql.append("(SELECT DE_EST FROM IDOSGD.TDTR_ESTADOS WHERE (CO_EST + DE_TAB) = (CASE VB.IN_VB WHEN 'B' THEN '0' ELSE VB.IN_VB END) + 'TDTV_PERSONAL_VB') DE_ES_DOC_EMI, ");
        sql.append("RE.NU_DIA_ATE, "); 
        sql.append("RE.DE_ASU, "); 
        sql.append("EX.NU_EXPEDIENTE NRO_EXPEDIENTE, ");
        sql.append("CONVERT(VARCHAR(10), EX.FE_EXP, 103) FE_EXP_CORTA, ");
        sql.append("[IDOSGD].[PK_SGD_DESCRIPCION_DE_PROCESO_EXP](EX.CO_PROCESO) DE_PROCESO_EXP, ");
        sql.append("RE.TI_CAP, ");
        sql.append("VB.OBS_VB DE_OBS ");
        sql.append("FROM IDOSGD.TDTV_REMITOS RE WITH (NOLOCK)  "); 
        sql.append("LEFT JOIN IDOSGD.TDTC_EXPEDIENTE EX  WITH (NOLOCK) ON RE.NU_ANN_EXP=EX.NU_ANN_EXP AND RE.NU_SEC_EXP=EX.NU_SEC_EXP, ");
        sql.append("IDOSGD.TDTV_PERSONAL_VB VB WITH (NOLOCK)  ");
        sql.append("WHERE RE.NU_ANN=? AND RE.NU_EMI=? ");
        sql.append("AND VB.NU_ANN=? AND VB.NU_EMI=? ");
        sql.append("AND VB.CO_DEP=? AND VB.CO_EMP_VB=? ");                
        sql.append("AND (RE.ES_DOC_EMI='7' OR (RE.TI_EMI='05' AND RE.ES_DOC_EMI='5'))");
        sql.append("AND RE.ES_ELI='0' ");
        sql.append("AND (RE.CO_GRU='1' OR RE.CO_GRU='2')");

        DocumentoVoBoBean docVobo=null;
        try {
            docVobo = this.jdbcTemplate.queryForObject(sql.toString(), BeanPropertyRowMapper.newInstance(DocumentoVoBoBean.class),
                    new Object[]{pnuAnn, pnuEmi, pnuAnn, pnuEmi, coDep, coEmpVb});
        } catch (EmptyResultDataAccessException e) {
            docVobo = null;
        } catch (Exception e) {
            System.out.println("nu_ann="+pnuAnn+","+"nu_emi="+pnuEmi);
            e.printStackTrace();
        }
        return docVobo;
    }
    
    @Override
    public String updObsVistoBueno(String nuAnn,String nuEmi,String coDep,String coEmp,String obs,String coUserMod, String esDocVb) {
        String vReturn = "NO_OK";
        StringBuffer sqlUpd = new StringBuffer();
        sqlUpd.append("UPDATE IDOSGD.TDTV_PERSONAL_VB ");
        sqlUpd.append("SET OBS_VB=?, ");
        sqlUpd.append("	CO_USE_MOD=?, ");
        sqlUpd.append("	FE_USE_MOD=CURRENT_TIMESTAMP, ");
        sqlUpd.append("	IN_VB=? ");
        sqlUpd.append("WHERE NU_ANN=? ");
        sqlUpd.append("AND NU_EMI=? ");
        sqlUpd.append("AND CO_DEP=? ");
        sqlUpd.append("AND CO_EMP_VB=? ");
        sqlUpd.append("AND IN_VB <> '1' ");
        
        try {
            this.jdbcTemplate.update(sqlUpd.toString(), new Object[]{obs,coUserMod,esDocVb,nuAnn,nuEmi,coDep,coEmp});
            vReturn = "OK";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn;
    }
    
    @Override
    public String updToEnviadoVistoBueno(String nuAnn,String nuEmi,String coDep,String coEmp,String esDocVobo,String coUserMod){
        String vReturn = "NO_OK";
        StringBuffer sqlUpd = new StringBuffer();
        sqlUpd.append("UPDATE IDOSGD.TDTV_PERSONAL_VB ");
        sqlUpd.append("SET IN_VB=?, ");
        sqlUpd.append("FE_VB=CURRENT_TIMESTAMP, ");
        sqlUpd.append("CO_USE_MOD=?, ");
        sqlUpd.append("FE_USE_MOD=CURRENT_TIMESTAMP, ");
        sqlUpd.append("OBS_VB=NULL ");
        sqlUpd.append("WHERE NU_ANN=? ");
        sqlUpd.append("AND NU_EMI=? ");
        sqlUpd.append("AND CO_DEP=? ");
        sqlUpd.append("AND CO_EMP_VB=? ");
        sqlUpd.append("AND IN_VB <> '1' ");
        
        try {
            this.jdbcTemplate.update(sqlUpd.toString(), new Object[]{esDocVobo,coUserMod,nuAnn,nuEmi,coDep,coEmp});
            vReturn = "OK";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn;        
    }
    
    @Override
    public String updToSinVistoBueno(String nuAnn,String nuEmi,String coDep,String coEmp,String esDocVobo,String coUserMod){
        String vReturn = "NO_OK";
        StringBuffer sqlUpd = new StringBuffer();
        sqlUpd.append("UPDATE IDOSGD.TDTV_PERSONAL_VB ");
        sqlUpd.append("SET IN_VB=?, ");
        sqlUpd.append("CO_USE_MOD=?, ");
        sqlUpd.append("FE_USE_MOD=CURRENT_TIMESTAMP, ");
        sqlUpd.append("OBS_VB=NULL, ");
        sqlUpd.append("FE_VB=NULL ");
        sqlUpd.append("WHERE NU_ANN=? ");
        sqlUpd.append("AND NU_EMI=? ");
        sqlUpd.append("AND CO_DEP=? ");
        sqlUpd.append("AND CO_EMP_VB=? ");
        sqlUpd.append("AND IN_VB='1' ");
        
        try {
            this.jdbcTemplate.update(sqlUpd.toString(), new Object[]{esDocVobo,coUserMod,nuAnn,nuEmi,coDep,coEmp});
            vReturn = "OK";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn;        
    }    
    
    @Override
    public String getDeObsEmpVoBo(String nuAnn, String nuEmi, String coDep, String coEmp){
        String deObs=null;
        StringBuffer sqlQry = new StringBuffer();
        sqlQry.append("SELECT VB.OBS_VB "); 
        sqlQry.append("FROM IDOSGD.TDTV_PERSONAL_VB VB  WITH (NOLOCK) ");
        sqlQry.append("WHERE VB.NU_ANN=? "); 
        sqlQry.append("AND VB.NU_EMI=? "); 
        sqlQry.append("AND VB.CO_DEP=? "); 
        sqlQry.append("AND VB.CO_EMP_VB=? ");
        sqlQry.append("AND VB.IN_VB='2' ");  
        
        try{
            deObs = this.jdbcTemplate.queryForObject(sqlQry.toString(), String.class, new Object[]{nuAnn,nuEmi,coDep,coEmp});
        }  catch (EmptyResultDataAccessException e) {
            deObs = null;
        }catch (Exception e) {
            e.printStackTrace();
        }       
        return deObs;        
    }
    
    @Override
    public String existeVistoBuenoDocAdm(String nuAnn, String nuEmi){
        String vResult = "1";// '0' no existe pendiente visto bueno.
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT COUNT(1) ");
        sql.append("FROM IDOSGD.TDTV_PERSONAL_VB VB  WITH (NOLOCK) ");
        sql.append("WHERE VB.NU_ANN=? ");
        sql.append("AND VB.NU_EMI=? ");
        sql.append("AND VB.IN_VB='1' ");
        
        try {
            vResult = this.jdbcTemplate.queryForObject(sql.toString(), String.class, new Object[]{nuAnn, nuEmi});
        } catch (Exception e) {
            e.printStackTrace();
        }        
        return vResult;
    }
    
    @Override
    public String existeVistoBuenoPendienteDocAdm(String nuAnn, String nuEmi){
        String vResult= "NO_OK";// '0' no existe pendiente visto bueno.
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT COUNT(1) "); 
        sql.append("FROM IDOSGD.TDTV_PERSONAL_VB VB  WITH (NOLOCK) ");
        sql.append("WHERE VB.NU_ANN=? "); 
        sql.append("AND VB.NU_EMI=? ");
        sql.append("AND VB.IN_VB <> '1' ");
        try {
            vResult = this.jdbcTemplate.queryForObject(sql.toString(), String.class, new Object[]{nuAnn, nuEmi});
        } catch (Exception e) {
            e.printStackTrace();
            vResult=e.getMessage().substring(0, 20);
        }        
        return vResult;        
    }
    
    @Override
    public String existePersonalVb(String nuAnn, String nuEmi, String coDep, String coEmpVb){
        String vResult= "0";// '0' no existe visto bueno.
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT COUNT(1) "); 
        sql.append("FROM IDOSGD.TDTV_PERSONAL_VB VB  WITH (NOLOCK) ");
        sql.append("WHERE VB.NU_ANN=? ");
        sql.append("AND VB.NU_EMI=? ");
        sql.append("AND VB.CO_DEP=? ");
        sql.append("AND VB.CO_EMP_VB=? ");
        try {
            vResult = this.jdbcTemplate.queryForObject(sql.toString(), String.class, new Object[]{nuAnn,nuEmi, coDep, coEmpVb});
        } catch (Exception e) {
            e.printStackTrace();
        }        
        return vResult;    
    }
    
    @Override
    public String updVistoBuenoPersonal(String nuAnn,String nuEmi,String coDep,String coEmp,String esDocVobo,String coUserMod){
        String vReturn = "NO_OK";
        StringBuffer sqlUpd = new StringBuffer();
        sqlUpd.append("UPDATE IDOSGD.TDTV_PERSONAL_VB ");
        sqlUpd.append("SET IN_VB=?, ");
        sqlUpd.append("CO_USE_MOD=?, ");
        sqlUpd.append("FE_USE_MOD=CURRENT_TIMESTAMP, ");
        sqlUpd.append("OBS_VB=NULL ");
        sqlUpd.append("WHERE NU_ANN=? ");
        sqlUpd.append("AND NU_EMI=? ");
        sqlUpd.append("AND CO_DEP=? ");
        sqlUpd.append("AND CO_EMP_VB=? ");
        sqlUpd.append("AND IN_VB <> '1' ");
        
        try {
            this.jdbcTemplate.update(sqlUpd.toString(), new Object[]{esDocVobo,coUserMod,nuAnn,nuEmi,coDep,coEmp});
            vReturn = "OK";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn;        
    }
    
//    @Override
//    public String updVistoBuenoPersonal(String nuAnn,String nuEmi,String esDocVobo,String coEmp){
//        String vReturn = "NO_OK";
//        StringBuffer sqlUpd = new StringBuffer();
//        sqlUpd.append("UPDATE TDTV_PERSONAL_VB VB\n" +
//                        "SET VB.IN_VB=?,VB.OBS_VB=NULL\n" +
//                        "WHERE VB.NU_ANN=?\n" +
//                        "AND VB.NU_EMI=?\n" +
//                        "AND VB.CO_EMP_VB <> ?\n" +
///*SIN VISTO BUENO*/     "AND VB.IN_VB <> '1'");
//        
//        try {
//            this.jdbcTemplate.update(sqlUpd.toString(), new Object[]{esDocVobo,nuAnn,nuEmi,coEmp});
//            vReturn = "OK";
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return vReturn;        
//    }  
    
    @Override
    public String updVistoBuenoPersonal(String nuAnn,String nuEmi){
        String vReturn = "NO_OK";
        StringBuffer sqlUpd = new StringBuffer();
        sqlUpd.append("UPDATE IDOSGD.TDTV_PERSONAL_VB ");
        sqlUpd.append("SET IN_VB='0', ");
        sqlUpd.append("OBS_VB=NULL ");
        sqlUpd.append("WHERE NU_ANN=? ");
        sqlUpd.append("AND NU_EMI=? ");
        sqlUpd.append("AND IN_VB='B' ");
        
        try {
            this.jdbcTemplate.update(sqlUpd.toString(), new Object[]{nuAnn, nuEmi});
            vReturn = "OK";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vReturn;        
    } 
    
    @Override
    public String getEsVobo(String nuAnn, String nuEmi, String coDep, String coEmpVb){
        String vResult = "1";// '1' con Visto Bueno.
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT VB.IN_VB "); 
        sql.append("FROM IDOSGD.TDTV_PERSONAL_VB VB WITH (NOLOCK)  ");
        sql.append("WHERE VB.NU_ANN=? ");
        sql.append("AND VB.NU_EMI=? ");
        sql.append("AND VB.CO_DEP=? ");
        sql.append("AND VB.CO_EMP_VB=? ");
        try {
            vResult = this.jdbcTemplate.queryForObject(sql.toString(), String.class, new Object[]{nuAnn,nuEmi,coDep, coEmpVb});
        } catch (Exception e) {
            e.printStackTrace();
        }        
        return vResult;           
    }
}
