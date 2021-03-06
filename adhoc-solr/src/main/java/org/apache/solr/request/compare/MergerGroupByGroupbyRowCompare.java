package org.apache.solr.request.compare;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.apache.solr.request.join.HigoJoinSort;

import com.alimama.mdrill.distinct.DistinctCount;
import com.alimama.mdrill.utils.UniqConfig;


public class MergerGroupByGroupbyRowCompare implements Comparator<GroupbyRow>,Serializable{
	private static Logger LOG = Logger.getLogger(MergerGroupByGroupbyRowCompare.class);
	private static final long serialVersionUID = 1L;
	private UniqTypeNum.SortType typenum=null;
	private boolean isdesc=true;
	private Integer fl_num=0;
	private compareInterface cmpobj=null;
	
	public MergerGroupByGroupbyRowCompare(String columntype,String[] groupby,String[] crossFs, String[] distFS,HigoJoinSort[] joinInvert,String fl,String type,boolean _isdesc) {
		this.isdesc=_isdesc;
		this.typenum=UniqTypeNum.parseType(type,fl,groupby,joinInvert) ;
		this.cmpobj=new CompareIndex();
		LOG.info("####"+this.typenum.toString());

		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.joincolumn))
		{
			this.fl_num=this.typenum.sortFieldNum;
			this.cmpobj=new CompareColumn();
			return ;
		}
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.index))
		{
			this.fl_num=0;
			this.cmpobj=new CompareIndex();
			return ;
		}
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.countall))
		{
			this.fl_num=0;
			this.cmpobj=new CompareCountall();
			return ;
		}
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.count))
		{
			this.fl_num=UniqTypeNum.foundIndex(crossFs, fl);
			this.cmpobj=new CompareCount();
			return ;
		}
		
		
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.dist))
		{
			this.fl_num=UniqTypeNum.foundIndex(distFS, fl);
			this.cmpobj=new CompareDist();
			return ;
		}
		
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.sum))
		{
			this.fl_num=UniqTypeNum.foundIndex(crossFs, fl);
			this.cmpobj=new CompareSum();
			return ;
		}
		
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.max))
		{
			this.fl_num=UniqTypeNum.foundIndex(crossFs, fl);
			this.cmpobj=new CompareMax();
			return ;
		}
		
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.min))
		{
			this.fl_num=UniqTypeNum.foundIndex(crossFs, fl);
			this.cmpobj=new CompareMin();
			return ;
		}
		
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.avg))
		{
			this.fl_num=UniqTypeNum.foundIndex(crossFs, fl);
			this.cmpobj=new CompareAvg();
			return ;
		}
		
		if(this.typenum.typeEnum.equals(UniqTypeNum.SortTypeEnum.column))
		{
			this.fl_num=UniqTypeNum.foundIndex(groupby, fl);
			if(columntype.equals("string"))
			{
				LOG.info("##cmp string##");
				this.cmpobj=new CompareColumn();
			}else{
				LOG.info("##cmp num##");
				this.cmpobj=new CompareColumnNum();
			}
			return ;
		}
		

	}

	public compareInterface getCmpobj() {
		return cmpobj;
	}

	@Override
	public int compare(GroupbyRow o1,
			GroupbyRow o2) {
		int cmp = cmpobj.compare(o1, o2);
		if(this.isdesc)
		{
			return cmp;
		}
		return cmp*-1;
	}
	
	public interface compareInterface
	{
		public ArrayList<Object> getCompareValue(ColumnKey o1);
		public int compare(GroupbyRow o1, GroupbyRow o2);
	}
	
	public class CompareIndex implements compareInterface
	{
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			ArrayList<Object> rtn=new ArrayList<Object>();
			rtn.add(0, o1.getkeyCrc());
			return rtn;
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			if(o1.getKey().isIscrc())
			{
				return UniqTypeNum.compare((Long)o1.getKey().getSort().get(0), (Long)o2.getKey().getSort().get(0));
			}
			
			String[] values1 =o1.getKey().getKey().split(UniqConfig.GroupJoinString());
			String[] values2 = o2.getKey().getKey().split(UniqConfig.GroupJoinString());
			return UniqTypeNum.compareDecode(values1, values2);
		}
	}
	
	public class CompareCountall implements compareInterface
	{
		CompareIndex index=new CompareIndex();
		
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			return index.getCompareValue(o1);
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			int cmp= UniqTypeNum.compare(o1.getValue(), o2.getValue());
			if(cmp==0)
			{
				cmp=index.compare(o1, o2);
			}
			return cmp;
		}
	}
	public class CompareCount implements compareInterface
	{
		CompareIndex index=new CompareIndex();
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			return index.getCompareValue(o1);
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			
			int cmp= UniqTypeNum.compare(getCompareValue(o1,fl_num),getCompareValue(o2,fl_num));
			if(cmp==0)
			{
				cmp=index.compare(o1, o2);
			}
			return cmp;
		}
		
		public double getCompareValue(GroupbyRow g, Integer field) {
			double cnt=g.getStatVal(field, 4);
			if(cnt<=0)
			{
				return 0;
			}
			return cnt;
		}
	}
	
	public class CompareDist implements compareInterface
	{
		CompareIndex index=new CompareIndex();
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			return index.getCompareValue(o1);
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			
			int cmp= UniqTypeNum.compare(getCompareValue(o1,fl_num),getCompareValue(o2,fl_num));
			if(cmp==0)
			{
				cmp=index.compare(o1, o2);
			}
			return cmp;
		}
		
		public long getCompareValue(GroupbyRow g, Integer field) {
			DistinctCount dst=g.dist.get(field);
			if(dst!=null)
			{
				return dst.getValue();
			}else{
				return 0l;
			}
		}
	}
	
	public class CompareSum implements compareInterface
	{
		CompareIndex index=new CompareIndex();
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			return index.getCompareValue(o1);
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			
			int cmp= UniqTypeNum.compare(getCompareValue(o1,fl_num),getCompareValue(o2,fl_num));
			if(cmp==0)
			{
				cmp=index.compare(o1, o2);
			}
			return cmp;
		}
		
		public double getCompareValue(GroupbyRow g, Integer field) {
			return g.getStatVal(field, 1);
		}
	}
	
	public class CompareMax implements compareInterface
	{
		CompareIndex index=new CompareIndex();
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			return index.getCompareValue(o1);
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			
			int cmp= UniqTypeNum.compare(getCompareValue(o1,fl_num),getCompareValue(o2,fl_num));
			if(cmp==0)
			{
				cmp=index.compare(o1, o2);
			}
			return cmp;
		}
		
		public double getCompareValue(GroupbyRow g, Integer field) {
			return g.getStatVal(field, 2);
		}
	}
	
	public class CompareMin implements compareInterface
	{
		CompareIndex index=new CompareIndex();
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			return index.getCompareValue(o1);
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			
			int cmp= UniqTypeNum.compare(getCompareValue(o1,fl_num),getCompareValue(o2,fl_num));
			if(cmp==0)
			{
				cmp=index.compare(o1, o2);
			}
			return cmp;
		}
		
		public double getCompareValue(GroupbyRow g, Integer field) {
			return g.getStatVal(field, 3);
		}
	}
	
	public class CompareAvg implements compareInterface
	{
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			return index.getCompareValue(o1);
		}
		CompareIndex index=new CompareIndex();
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			
			int cmp= UniqTypeNum.compare(getCompareValue(o1,fl_num),getCompareValue(o2,fl_num));
			if(cmp==0)
			{
				cmp=index.compare(o1, o2);
			}
			return cmp;
		}
		
		public double getCompareValue(GroupbyRow g,Integer field) {
			double cnt=g.getStatVal(field, 4);
			if(cnt<=0)
			{
				return 0;
			}
			return g.getStatVal(field, 1)/cnt;
		}
	}
	
	public class CompareColumn implements compareInterface
	{
		CompareIndex index=new CompareIndex();
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			ArrayList<Object> list=index.getCompareValue(o1);
			String[] values1 =o1.getKey().split(UniqConfig.GroupJoinString(),-1);
			
			list.add(1, values1[fl_num]);
			
			return list;
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			int cmp=0;
			if(o1.getKey().isIscrc())
			{
				cmp= UniqTypeNum.compareDecode((String)o1.getKey().getSort().get(1),(String) o2.getKey().getSort().get(1));
			}else{
				String[] values1 =o1.getKey().getKey().split(UniqConfig.GroupJoinString(),-1);
				String[] values2 = o2.getKey().getKey().split(UniqConfig.GroupJoinString(),-1);
				cmp= UniqTypeNum.compareDecode(values1[fl_num],values2[fl_num]);
			}
			if(cmp==0)
			{
				cmp=index.compare(o1, o2);
			}
			
			return cmp;
		}
	}
	
	public class CompareColumnNum implements compareInterface
	{
		CompareIndex index=new CompareIndex();
		public ArrayList<Object> getCompareValue(ColumnKey o1)
		{
			ArrayList<Object> list=index.getCompareValue(o1);
			String[] values1 =o1.getKey().split(UniqConfig.GroupJoinString());
			list.add(1, UniqTypeNum.filterUnNumber(values1[fl_num]));
			return list;
		}
		@Override
		public int compare(GroupbyRow o1, GroupbyRow o2) {
			
		
			int cmp=0;
			if(o1.getKey().isIscrc())
			{
				cmp= UniqTypeNum.compare((Double)o1.getKey().getSort().get(1), (Double)o2.getKey().getSort().get(1));
			}else{
				String[] values1 =o1.getKey().getKey().split(UniqConfig.GroupJoinString(),-1);
				String[] values2 = o2.getKey().getKey().split(UniqConfig.GroupJoinString(),-1);
				cmp= UniqTypeNum.compareDecodeNum(values1[fl_num],values2[fl_num]);
				if(cmp==0)
				{
					cmp=index.compare(o1, o2);
				}
			}
			return cmp;
		}
	}
	
}
