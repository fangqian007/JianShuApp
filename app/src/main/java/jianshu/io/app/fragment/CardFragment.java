package jianshu.io.app.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.tsz.afinal.FinalBitmap;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import jianshu.io.app.ArticleActivity;
import jianshu.io.app.R;
import jianshu.io.app.model.RecommendationItem;
import jianshu.io.app.model.StatePool;
import jianshu.io.app.model.datapool.DataPool;
import jianshu.io.app.util.RecommendationAsyncTask;
import jianshu.io.app.widget.EndlessCardListView;
import jianshu.io.app.widget.EndlessListener;
import jianshu.io.app.widget.HotCard;
import jianshu.io.app.widget.LoadingTextView;

/**
 * Created by Administrator on 2014/5/7.
 */
public class CardFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, EndlessListener {

  public static CardFragment newInstance(String url) {
    CardFragment fragment = new CardFragment();
    Bundle bundle = new Bundle();
    bundle.putString("url", url);
    fragment.setArguments(bundle);
    return fragment;
  }

  FinalBitmap fb;
  EndlessCardListView mListView;
  SwipeRefreshLayout mRefreshLayout;
  CardArrayAdapter mAdapter;
  LoadingTextView mFooter;
  DataPool mPool;
  View mEmptyView;
  boolean mIsEmpty;
  String mUrl;
  int[] mListViewState;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = getActivity().getLayoutInflater().inflate(R.layout.hot, null);


    final Activity activity = getActivity();
    this.fb = FinalBitmap.create(activity);

    mUrl = getArguments().getString("url");
    Object[] states = StatePool.getInstance().getState(getActivity(), mUrl);
    mPool = (DataPool)states[0];
    mAdapter = (CardArrayAdapter)states[1];
    if(states[2] != null) {
      mListViewState = (int[])states[2];
    }

    mEmptyView = getActivity().getLayoutInflater().inflate(R.layout.empty_pull, null);
    mListView = (EndlessCardListView) (view.findViewById(R.id.hotlist));
    mListView.setListener(this);
    mFooter = (LoadingTextView) activity.getLayoutInflater().inflate(R.layout.footer, null);
    mListView.setFooter(mFooter);
    mListView.setAdapter(mAdapter);

    mRefreshLayout = (SwipeRefreshLayout) (view.findViewById(R.id.ptr_layout));
    mRefreshLayout.setColorScheme(R.color.jianshu, R.color.card_list_gray, R.color.jianshu, R.color.card_list_gray);
    mRefreshLayout.setOnRefreshListener(this);

    if(mAdapter.getCount() == 0) {
      mRefreshLayout.setRefreshing(true);
      onRefresh();
    } else {
      mAdapter.notifyDataSetChanged();
    }

    return view;
  }

  @Override
  public void onViewStateRestored(Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if(mListViewState != null) {
      mListView.setSelectionFromTop(mListViewState[0], mListViewState[1]);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    int index = mListView.getFirstVisiblePosition();
    View v = mListView.getChildAt(0);
    int top = (v == null) ? 0 : v.getTop();
    StatePool.getInstance().putListViewState(mUrl, new int[]{index, top});
  }

  private Card[] initCard(Context context, RecommendationItem[] data) {
    Card[] result = new Card[data.length];
    int i = 0;
    for(RecommendationItem item : data) {
      HotCard card = new HotCard(context, item, this.fb);
      result[i++] = card;
      card.addPartialOnClickListener(Card.CLICK_LISTENER_CONTENT_VIEW, new Card.OnCardClickListener() {
        @Override
        public void onClick(Card card, View view) {
          RecommendationItem item = ((HotCard)card).getItem();
          Intent intent = new Intent(getActivity(), ArticleActivity.class);
          intent.putExtra("url", item.getUrl());
          intent.putExtra("title", item.getTitle());
          intent.putExtra("summary", item.getSummary());
          intent.putExtra("author", item.getAuthor());
          startActivity(intent);
          getActivity().overridePendingTransition(R.anim.slide_in_left, 0);
    }
  });
    }
    return result;
  }

  @Override
  public void onRefresh() {
    new RecommendationAsyncTask(true, this.mPool, new RecommendationAsyncTask.OnPostExecuteTask() {
      @Override
      public void run(RecommendationItem[] data) {
        mRefreshLayout.setRefreshing(false);
        Context context = getActivity();
        if(context == null) {
          return;
        }
        if(data != null) {
          if(mIsEmpty) {
            mIsEmpty = false;
            mRefreshLayout.removeView(mEmptyView);
            mRefreshLayout.addView(mListView);
          } else {
            mAdapter.clear();
          }
          mAdapter.addAll(initCard(context, data));
        } else {
          if(context != null) {
            Toast.makeText(context, ":( 加载失败，请重试", Toast.LENGTH_LONG).show();
          }
          if(mAdapter.getCount() == 0 && !mIsEmpty) {
            mIsEmpty = true;
            mRefreshLayout.removeView(mListView);
            mRefreshLayout.addView(mEmptyView);
          }
        }
      }
    }).execute();
  }

  @Override
  public boolean isAtTheEnd() {
    return mPool.isAtTheEnd();
  }

  @Override
  public void onScrollEnd() {
    mFooter.startAnimation();
    new RecommendationAsyncTask(false, this.mPool, new RecommendationAsyncTask.OnPostExecuteTask() {
      @Override
      public void run(RecommendationItem[] data) {
        mFooter.endAnimation();
        mListView.notifyNewDataLoaded();
        Context context = getActivity();
        if(context == null) {
          return;
        }
        if (data != null) {
          mAdapter.addAll(initCard(context, data));
        } else {
          Toast.makeText(context, ":( 加载失败，请重试", Toast.LENGTH_LONG).show();
        }
      }
    }).execute();
  }

  public interface HotFragmentListner {
    void onRefreshStart();
    void onRefreshEnd();
  }
}