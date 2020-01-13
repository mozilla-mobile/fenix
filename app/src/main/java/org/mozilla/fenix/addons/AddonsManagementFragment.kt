/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_add_ons_management.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManagerException
import org.mozilla.fenix.R
import org.mozilla.fenix.addons.AddonsManagementFragment.CustomViewHolder.AddonViewHolder
import org.mozilla.fenix.addons.AddonsManagementFragment.CustomViewHolder.SectionViewHolder
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar

/**
 * Fragment use for managing add-ons.
 */
@Suppress("TooManyFunctions")
class AddonsManagementFragment : Fragment(), View.OnClickListener {
    private lateinit var recyclerView: RecyclerView
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_ons_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindRecyclerView(view)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_addons))
    }

    override fun onStart() {
        super.onStart()
        findPreviousDialogFragment()?.let { dialog ->
            dialog.onPositiveButtonClicked = onPositiveButtonClicked
        }
    }

    private fun bindRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.add_ons_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        scope.launch {
            try {
                val addons = requireContext().components.addonManager.getAddons()

                scope.launch(Dispatchers.Main) {
                    val adapter = AddonsAdapter(
                        this@AddonsManagementFragment,
                        addons
                    )
                    recyclerView.adapter = adapter
                }
            } catch (e: AddonManagerException) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(activity, "Failed to query Add-ons!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * An adapter for displaying add-on items.
     */
    inner class AddonsAdapter(
        private val clickListener: View.OnClickListener,
        addons: List<Addon>
    ) : RecyclerView.Adapter<CustomViewHolder>() {
        private val items: List<Any>

        init {
            items = createListWithSections(addons)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            return if (viewType == VIEW_HOLDER_TYPE_ADDON) {
                createAddonViewHolder(parent)
            } else {
                createSectionViewHolder(parent)
            }
        }

        private fun createSectionViewHolder(parent: ViewGroup): CustomViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.addons_section_item, parent, false)
            val titleView = view.findViewById<TextView>(R.id.title)

            return SectionViewHolder(view, titleView)
        }

        private fun createAddonViewHolder(parent: ViewGroup): AddonViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.add_ons_item, parent, false)
            val iconView = view.findViewById<ImageView>(R.id.add_on_icon)
            val titleView = view.findViewById<TextView>(R.id.add_on_name)
            val summaryView = view.findViewById<TextView>(R.id.add_on_description)
            val ratingView = view.findViewById<RatingBar>(R.id.rating)
            val userCountView = view.findViewById<TextView>(R.id.users_count)
            val addButton = view.findViewById<ImageView>(R.id.add_button)
            return AddonViewHolder(
                view,
                iconView,
                titleView,
                summaryView,
                ratingView,
                userCountView,
                addButton
            )
        }

        override fun getItemCount() = items.size

        override fun getItemViewType(position: Int): Int {
            val isSection = items[position] !is Addon
            return if (isSection) VIEW_HOLDER_TYPE_SECTION else VIEW_HOLDER_TYPE_ADDON
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val item = items[position]

            when (holder) {
                is SectionViewHolder -> bindSection(holder, item as Section)
                is AddonViewHolder -> bindAddon(holder, item as Addon)
            }
        }

        private fun bindSection(holder: SectionViewHolder, section: Section) {
            holder.titleView.setText(section.title)
        }

        private fun bindAddon(holder: AddonViewHolder, addon: Addon) {
            val context = holder.itemView.context
            addon.rating?.let {
                val userCount = context.getString(R.string.mozac_feature_addons_user_rating_count)
                val ratingContentDescription =
                    context.getString(R.string.mozac_feature_addons_rating_content_description)
                holder.ratingView.contentDescription =
                    String.format(ratingContentDescription, it.average)
                holder.ratingView.rating = it.average
                holder.userCountView.text = String.format(userCount, getFormattedAmount(it.reviews))
            }

            holder.titleView.text = addon.translatableName.translate()
            holder.summaryView.text = addon.translatableSummary.translate()
            holder.itemView.tag = addon
            holder.itemView.setOnClickListener(clickListener)
            holder.addButton.isVisible = !addon.isInstalled()
            val listener = if (!addon.isInstalled()) {
                clickListener
            } else {
                null
            }
            holder.addButton.setOnClickListener(listener)

            scope.launch {
                val iconBitmap = context.components.addonCollectionProvider.getAddonIconBitmap(addon)

                iconBitmap?.let {
                    MainScope().launch {
                        holder.iconView.setImageBitmap(it)
                    }
                }
            }
        }

        private fun createListWithSections(addons: List<Addon>): List<Any> {
            // We want to have the installed add-ons first in the list.
            val sortedAddons = addons.sortedBy { !it.isInstalled() }

            val itemsWithSections = ArrayList<Any>()
            val shouldAddInstalledSection = sortedAddons.first().isInstalled()
            var isRecommendedSectionAdded = false

            if (shouldAddInstalledSection) {
                itemsWithSections.add(Section(R.string.mozac_feature_addons_installed_section))
            }

            sortedAddons.forEach { addon ->
                if (!isRecommendedSectionAdded && !addon.isInstalled()) {
                    itemsWithSections.add(Section(R.string.mozac_feature_addons_recommended_section))
                    isRecommendedSectionAdded = true
                }

                itemsWithSections.add(addon)
            }
            return itemsWithSections
        }
    }

    /**
     * A base view holder.
     */
    sealed class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        /**
         * A view holder for displaying section items.
         */
        class SectionViewHolder(
            view: View,
            val titleView: TextView
        ) : CustomViewHolder(view)

        /**
         * A view holder for displaying add-on items.
         */
        class AddonViewHolder(
            view: View,
            val iconView: ImageView,
            val titleView: TextView,
            val summaryView: TextView,
            val ratingView: RatingBar,
            val userCountView: TextView,
            val addButton: ImageView
        ) : CustomViewHolder(view)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.add_button -> {
                val addon = (((view.parent) as View).tag as Addon)
                showPermissionDialog(addon)
            }
            R.id.add_on_item -> {
                val addon = view.tag as Addon
                if (addon.isInstalled()) {
                    showInstalledAddonDetailsFragment(addon)
                } else {
                    showDetailsFragment(addon)
                }
            }
            else -> {
            }
        }
    }

    private fun showInstalledAddonDetailsFragment(addon: Addon) {
        val directions =
            AddonsManagementFragmentDirections.actionAddonsManagementFragmentToInstalledAddonDetails(
                addon
            )
        Navigation.findNavController(requireView()).navigate(directions)
    }

    private fun showDetailsFragment(addon: Addon) {
        val directions =
            AddonsManagementFragmentDirections.actionAddonsManagementFragmentToAddonDetailsFragment(
                addon
            )
        Navigation.findNavController(requireView()).navigate(directions)
    }

    private fun isAlreadyADialogCreated(): Boolean {
        return findPreviousDialogFragment() != null
    }

    private fun findPreviousDialogFragment(): PermissionsDialogFragment? {
        return parentFragmentManager.findFragmentByTag(PERMISSIONS_DIALOG_FRAGMENT_TAG) as? PermissionsDialogFragment
    }

    private fun showPermissionDialog(addon: Addon) {
        val dialog = PermissionsDialogFragment.newInstance(
            addon = addon,
            title = addon.translatableName.translate(),
            permissions = addon.translatePermissions(),
            promptsStyling = PermissionsDialogFragment.PromptsStyling(
                gravity = Gravity.BOTTOM,
                shouldWidthMatchParent = true
            ),
            onPositiveButtonClicked = onPositiveButtonClicked
        )

        if (!isAlreadyADialogCreated()) {
            dialog.show(parentFragmentManager, PERMISSIONS_DIALOG_FRAGMENT_TAG)
        }
    }

    private val onPositiveButtonClicked: ((Addon) -> Unit) = { addon ->
        addonProgressOverlay.visibility = View.VISIBLE
        requireContext().components.addonManager.installAddon(
            addon,
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    "Successfully installed: ${it.translatableName.translate()}",
                    Toast.LENGTH_SHORT
                ).show()

                this@AddonsManagementFragment.view?.let { view ->
                    bindRecyclerView(view)
                }

                addonProgressOverlay.visibility = View.GONE
            },
            onError = { _, _ ->
                Toast.makeText(
                    requireContext(),
                    "Failed to install: ${addon.translatableName.translate()}",
                    Toast.LENGTH_SHORT
                ).show()

                addonProgressOverlay.visibility = View.GONE
            }
        )
    }

    companion object {
        private const val PERMISSIONS_DIALOG_FRAGMENT_TAG = "ADDONS_PERMISSIONS_DIALOG_FRAGMENT"
        private const val VIEW_HOLDER_TYPE_SECTION = 0
        private const val VIEW_HOLDER_TYPE_ADDON = 1
    }

    private inner class Section(@StringRes val title: Int)
}
