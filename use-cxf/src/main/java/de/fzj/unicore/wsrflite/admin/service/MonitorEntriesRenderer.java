package de.fzj.unicore.wsrflite.admin.service;

import java.util.Calendar;
import java.util.Map;

import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.utils.MetricUtils;
import de.fzj.unicore.wsrflite.xmlbeans.MetricValueDocument;
import de.fzj.unicore.wsrflite.xmlbeans.MetricValueDocument.MetricValue;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class MonitorEntriesRenderer extends ValueRenderer{

	public MonitorEntriesRenderer(Resource parent){
		super(parent, MetricValueDocument.type.getDocumentElementName());
	}
	
	@Override
	protected MetricValueDocument[] getValue() {
		Calendar c = Calendar.getInstance();
		Map<String,String>values = MetricUtils.getValues(parent.getKernel().getMetricRegistry());
		MetricValueDocument[] metricValues = new MetricValueDocument[values.size()];
		int i=0;
		for(Map.Entry<String, String> entry: values.entrySet()) {
			MetricValueDocument res = MetricValueDocument.Factory.newInstance();
			MetricValue mv = res.addNewMetricValue();
			mv.setName(entry.getKey());
			mv.setTimestamp(c);
			mv.setValue(entry.getValue());
			metricValues[i] = res;
			i++;
		}
		return metricValues;
	}

}
