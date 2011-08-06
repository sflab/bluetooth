package com.sflab.common;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class BindViewAdapter<E> extends BaseAdapter {
	public BindViewAdapter(Context context, int layoutRes, ViewBinder<E> viewBinder) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mLayoutRes = layoutRes;
		mViewBinder = viewBinder;
	}

	public void setSource(List<E> list) {
		mList = list;
	}

	public E itemForPosition(int position) {
		if (mList == null) {
			return null;
		}

		return mList.get(position);
	}

	public int getCount() {
		return mList != null ? mList.size() : 0;
	}

	public Object getItem(int position) {
		return itemForPosition(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = mInflater.inflate(mLayoutRes, parent, false);
		} else {
			view = convertView;
		}
		mViewBinder.bind(view, mList.get(position));
		return view;
	}

	private final LayoutInflater mInflater;
	private final int mLayoutRes;
	private final ViewBinder<E> mViewBinder;
	private List<E> mList;

	public interface ViewBinder<E> {
		View bind(View view, E item);
	}
}
