package ewing.dao.impl;

import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import ewing.application.query.BasisDao;
import ewing.application.query.Page;
import ewing.application.query.QueryUtils;
import ewing.application.query.Where;
import ewing.common.vo.DictionaryNode;
import ewing.common.vo.FindDictionaryParam;
import ewing.dao.DictionaryDao;
import ewing.dao.entity.Dictionary;
import ewing.dao.query.QDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据字典数据访问实现。
 */
@Repository
public class DictionaryDaoImpl extends BasisDao<QDictionary, Dictionary> implements DictionaryDao {

    @Autowired
    private SQLQueryFactory queryFactory;

    private QDictionary qAllDictionary = new QDictionary("all");

    @Override
    public Page<Dictionary> findWithSubDictionary(FindDictionaryParam findDictionaryParam) {
        // 结果条数以根字典为准
        SQLQuery<Dictionary> rootQuery = queryFactory.selectDistinct(qDictionary)
                .from(qDictionary)
                .leftJoin(qAllDictionary)
                .on(qDictionary.dictionaryId.eq(qAllDictionary.rootId))
                .where(qDictionary.dictionaryId.eq(qDictionary.rootId))
                // 搜索条件，支持子字典名称搜索
                .where(Where.hasText(findDictionaryParam.getName(), qAllDictionary.name::contains))
                .where(Where.hasText(findDictionaryParam.getValue(), qAllDictionary.value::contains));
        // 先查询根字典的总数
        long total = rootQuery.fetchCount();

        // 关联查根字典下的所有子字典项
        rootQuery.limit(findDictionaryParam.getLimit()).offset(findDictionaryParam.getOffset());
        List<Dictionary> dictionaries = queryFactory.selectDistinct(qAllDictionary)
                .from(rootQuery.as(qDictionary))
                .leftJoin(qAllDictionary)
                .on(qDictionary.dictionaryId.eq(qAllDictionary.rootId))
                .fetch();

        return new Page<>(total, dictionaries);
    }

    @Override
    public List<DictionaryNode> findRootSubDictionaries(String[] rootValues) {
        return queryFactory.selectDistinct(
                QueryUtils.fitBean(DictionaryNode.class, qAllDictionary))
                .from(qDictionary)
                .join(qAllDictionary)
                .on(qDictionary.dictionaryId.eq(qAllDictionary.rootId))
                .where(qDictionary.value.in(rootValues))
                .fetch();
    }
}
